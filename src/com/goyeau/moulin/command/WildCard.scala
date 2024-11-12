package com.goyeau.moulin.command

import com.goyeau.moulin.Moulin
import com.goyeau.moulin.command.WildCard.*
import scala.reflect.NameTransformer
import scala.util.Try
import scala.Dynamic
import scala.language.dynamics
import scala.reflect.ensureAccessible
import scala.quoted.*
import scala.annotation.experimental

// trait WildCard:
//   @experimental transparent inline def any: CombinedModules           = this.any
//   @experimental transparent inline def anyOf[Module]: CombinedModules = this.anyOf[Module]

//   @experimental transparent inline def all: CombinedModules           = this.all
//   @experimental transparent inline def allOf[Module]: CombinedModules = this.allOf[Module]

/** Provides the `all` and `any` to run functions on all available objects on first level or recursively.
  */
object WildCard:
  extension [Parent](inline parent: Parent)
    /** Gives the ability to call functions or fields on any available objects declared on `parent`.
      *
      * For example given the following project setup:
      * ```
      * object project:
      *   object module1 extends ScalaModule
      *   object module2 extends ScalaModule
      * ```
      * You could call `project.any.compile()` to run the `compile()` functions on any modules declared on the first
      * level in `project`.
      */
    @experimental transparent inline def any: CombinedModules = anyOf[Any]

    /** Gives the ability to call functions or fields on any available objects extending `Module` declared on `parent`.
      *
      * For example given the following project setup:
      * ```
      * object project:
      *   object module1 extends ScalaModule
      *   object module2 extends JavaModule
      * ```
      * You could call `project.anyOf[ScalaModule].compile()` to run the `compile()` functions on any modules extending
      * `ScalaModule` declared on the first level in `project`.
      */
    @experimental transparent inline def anyOf[Module]: CombinedModules =
      ${ combineModuleMacro[Parent, Module]('parent, "any") }

    /** Gives the ability to call functions or fields on all available objects declared on `parent` and it's submodules
      * recusively.
      *
      * For example given the following project setup:
      * ```
      * object project:
      *   object module1 extends ScalaModule
      *   object module2 extends ScalaModule:
      *     object nested:
      *       def compile() = println("Compiling")
      * ```
      * You could call `project.all.compile()` to run the `compile()` functions on all modules declared in `project` and
      * its submodules such as `nested`.
      */
    @experimental transparent inline def all: CombinedModules = allOf[Any]

    /** Gives the ability to call functions or fields on all available objects extending `Module` declared on `parent`
      * and it's submodules recusively.
      *
      * For example given the following project setup:
      * ```
      * object project:
      *   object module1 extends ScalaModule
      *   object module2 extends JavaModule:
      *     object nested extends ScalaModule
      * ```
      * You could call `project.allOf[ScalaModule].compile()` to run the `compile()` functions on all modules extending
      * `Module` declared in `project` and its submodules such as `nested`.
      */
    @experimental transparent inline def allOf[Module]: CombinedModules =
      ${ combineModuleMacro[Parent, Module]('parent, "all") }

  @experimental
  def combineModuleMacro[Parent: Type, Module: Type](using Quotes)(
      parent: Expr[Parent],
      anyAll: "any" | "all"
  ): Expr[CombinedModules] =
    import quotes.reflect.*

    val parentSymbol = parent.asTerm.underlying.symbol
    val modules = anyAll match
      case "any" => anyModules[Module](parentSymbol)
      case "all" => allModules[Module](parentSymbol)
    val combinedModuleSymbol     = combineModuleSymbols(Symbol.spliceOwner, "combined", modules)
    val combinedModuleDefinition = combineModuleDefinitions(combinedModuleSymbol, modules)
    val code = Block(combinedModuleDefinition.toList, Ref(combinedModuleSymbol)).asExprOf[CombinedModules]
    // report.errorAndAbort(code.show)
    code

  /** Get all modules recusively under parent that extends Module */
  private def allModules[Module: Type](using Quotes)(parent: quotes.reflect.Symbol): List[quotes.reflect.Symbol] =
    anyModules[Module](parent) ++ anyModules[Any](parent).flatMap(allModules[Module])

  /** Get any modules directly under parent that extends Module */
  private def anyModules[Module: Type](using Quotes)(parent: quotes.reflect.Symbol): List[quotes.reflect.Symbol] =
    import quotes.reflect.*

    parent.fieldMembers.filter(field =>
      field.flags.is(Flags.Module) && field.termRef <:< TypeRepr.of[Module] &&
        !field.flags.is(Flags.Private) && !field.flags.is(Flags.Protected) &&
        !(TypeRepr.of[Moulin] <:< field.owner.termRef)
    )

  /** Group members of multiple modules by their signatures */
  private def groupedMembers(using Quotes)(modules: List[quotes.reflect.Symbol]) =
    import quotes.reflect.*

    modules
      .flatMap(module =>
        (module.fieldMembers ++ module.methodMembers)
          .filterNot(member =>
            member.isClassConstructor ||
              TypeRepr.of[Object] <:< member.owner.termRef ||
              member.isTypeDef || member.isClassDef || // Forward only values, not types and class definitions
              // member.flags.is(Flags.Inline) || // TODO: Should we exclude inlined members?
              // member.flags.is(Flags.Exported) || // TODO: Should we exclude exported members?
              member.flags.is(Flags.Private) ||
              member.flags.is(Flags.PrivateLocal) ||
              member.flags.is(Flags.Protected)
          )
          .map(member => Select(Ref(module), member))
      )
      .groupBy(member => (member.name, member.signature.fold(List.empty)(_.paramSigs)))

  /** Creates a new module Symbol that combines multiple module Symbols in one */
  @experimental
  private def combineModuleSymbols(using Quotes)(
      owner: quotes.reflect.Symbol,
      name: String,
      modules: List[quotes.reflect.Symbol]
  ): quotes.reflect.Symbol =
    import quotes.reflect.*

    def members(owner: Symbol) = groupedMembers(modules)
      .flatMap:
        case ((name, _), members) if members.forall(_.symbol.flags.is(Flags.Module)) =>
          val module = combineModuleSymbols(owner, name, members.map(_.symbol))
          List(module.moduleClass, module)

        case ((name, _), members) if members.forall(_.symbol.isDefDef) && name.contains("$default$") =>
          List(
            Symbol.newMethod(
              owner,
              name,
              members.head.symbol.info,
              Flags.Private,
              Symbol.noSymbol
            )
          )

        case ((name, _), members) if members.forall(_.symbol.isDefDef) =>
          def getResultType(info: TypeRepr): TypeRepr = info match
            case MethodType(_, _, resultType) => getResultType(resultType)
            case PolyType(_, _, resultType)   => getResultType(resultType)
            case ByNameType(resultType)       => getResultType(resultType)
            case resultType                   => resultType

          val paramSymss       = members.head.symbol.paramSymss
          val tupledResultType = tupleTypes(members.map(member => getResultType(member.symbol.info)))
          val tupledMethodType = paramSymss.reverse.foldLeft(tupledResultType)((partiallyAppliedMethodType, params) =>
            MethodType(params.map(_.name))(_ => params.map(_.info), _ => partiallyAppliedMethodType)
          )
          val hasDefault = members.flatMap(_.symbol.paramSymss.flatten).exists(_.flags.is(Flags.HasDefault))

          List(
            Symbol.newVal(owner, paramNamesName(name), TypeRepr.of[Seq[String]], Flags.Private, Symbol.noSymbol),
            Symbol.newMethod(
              owner,
              name,
              tupledMethodType,
              if hasDefault then Flags.Private else Flags.EmptyFlags,
              Symbol.noSymbol
            )
          )

        case ((name, _), members) if members.forall(_.symbol.isValDef) =>
          List(Symbol.newVal(owner, name, tupleTypes(members.map(_.symbol.info)), Flags.EmptyFlags, Symbol.noSymbol))

        case ((name, _), members) =>
          List.empty
      .toList

    Symbol.newModule(
      owner,
      name,
      Flags.EmptyFlags,
      Flags.EmptyFlags,
      List(TypeRepr.of[Object], TypeRepr.of[CombinedModules]),
      members,
      Symbol.noSymbol
    )
  end combineModuleSymbols

  /** Creates the object definition for the combined module Symbol created with `combineModuleSymbols` */
  @experimental
  private def combineModuleDefinitions(using Quotes)(
      combinedModule: quotes.reflect.Symbol,
      originalMembers: List[quotes.reflect.Symbol]
  ): (quotes.reflect.ValDef, quotes.reflect.ClassDef) =
    import quotes.reflect.*

    val statements = groupedMembers(originalMembers)
      .flatMap:
        case ((name, _), members) if members.forall(_.symbol.flags.is(Flags.Module)) =>
          combineModuleDefinitions(combinedModule.declaredField(name), members.map(_.symbol)).toList

        case ((name, signature), members) if members.forall(_.symbol.isDefDef) && name.contains("$default$") =>
          val symbol = combinedModule.declarations.find(d => d.name == name && d.signature.paramSigs == signature).get
          List(DefDef(symbol, _ => members.headOption))

        case ((name, signature), members) if members.forall(_.symbol.isDefDef) =>
          val symbol = combinedModule.declarations.find(d => d.name == name && d.signature.paramSigs == signature).get
          def body(params: List[List[Tree]]) =
            val forwardCalls = members.map(_.appliedToArgss(params.map(_.map(_.asExpr.asTerm))).asExpr)
            Some(Expr.ofTupleFromSeq(forwardCalls).asTerm)

          List(
            ValDef(
              combinedModule.declarations.find(_.name == paramNamesName(name)).get,
              Some(Expr.ofSeq(members.head.symbol.paramSymss.flatten.map(param => Expr(param.name))).asTerm)
            ),
            DefDef(symbol, body)
          )

        case ((name, _), members) if members.forall(_.symbol.isValDef) =>
          List(ValDef(combinedModule.declaredField(name), Some(Expr.ofTupleFromSeq(members.map(_.asExpr)).asTerm)))

        case _ => List.empty

    val parents = List(TypeTree.of[Object], TypeTree.of[CombinedModules])
    ClassDef.module(combinedModule, parents, body = statements.toList)
  end combineModuleDefinitions

  /** Tuple list of types
    *
    * Example: tupleTypes(List(TypeRepr.of[String], TypeRepr.of[Int])) == TypeRepr.of[(String, Int)]
    */
  def tupleTypes(using Quotes)(elements: List[quotes.reflect.TypeRepr]): quotes.reflect.TypeRepr = elements match
    case Nil          => quotes.reflect.TypeRepr.of[EmptyTuple]
    case head :: tail => quotes.reflect.TypeRepr.of[*:].appliedTo(List(head, tupleTypes(tail)))

  private def paramNamesName(name: String) = s"$name$$paramnames$$"

  /** Facade for modules that combines multiple modules into one. */
  trait CombinedModules extends Selectable with Dynamic:
    protected def selectedValue: Any = this

    /** Select member with given name */
    def selectDynamic(name: String): Any =
      try
        val field = selectedValue.getClass.getField(NameTransformer.encode(name))
        val _     = ensureAccessible(field)
        field.get(selectedValue)
      catch
        case ex: NoSuchFieldException =>
          applyDynamic(name)()

    /** Select method and apply to arguments.
      *
      * @param name
      *   The name of the selected method
      * @param paramTypes
      *   The class tags of the selected method's formal parameter types
      * @param args
      *   The arguments to pass to the selected method
      */
    def applyDynamic(name: String, paramTypes: Class[?]*)(args: Any*): Any =
      applyDynamicNamed(name)(args.map(("", _))*)

    /** Select method and apply to arguments with their names.
      *
      * @param name
      *   The name of the selected method
      * @param paramTypes
      *   The class tags of the selected method's formal parameter types
      * @param namedArgs
      *   The arguments to pass to the selected method with their names
      */
    def applyDynamicNamed(name: String)(namedArgs: (String, Any)*): Any =
      val paramNames = Try:
        val paramNamesField = selectedValue.getClass.getDeclaredField(paramNamesName(NameTransformer.encode(name)))
        paramNamesField.setAccessible(true)
        paramNamesField.get(selectedValue).asInstanceOf[Seq[String]]
      .getOrElse:
        Seq.empty

      val nameToArgs = paramNames
        .zip(namedArgs)
        .map:
          case (name, (maybeName, v)) => (if maybeName.isEmpty then name else maybeName, v)
        .toMap
      val args = paramNames.zipWithIndex.map: (paramName, i) =>
        nameToArgs
          .get(paramName)
          .getOrElse:
            val defaultMethod = selectedValue.getClass.getDeclaredMethod(s"$name$$default$$${i + 1}")
            defaultMethod.setAccessible(true)
            defaultMethod.invoke(selectedValue)

      val method = selectedValue.getClass.getDeclaredMethods
        .find(method => method.getName == NameTransformer.encode(name) && method.getParameterCount == args.size)
        .getOrElse(
          throw NoSuchMethodException(
            s"Unable to find the method $name with ${args.size} parameters in ${selectedValue.getClass.getDeclaredMethods.toSeq}"
          )
        )
      method.setAccessible(true) // This is private to avoid Selectable to take precedence
      method.invoke(selectedValue, args.asInstanceOf[Seq[AnyRef]]*)
  end CombinedModules
