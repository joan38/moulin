package com.goyeau.moulin.cache

import scala.quoted.*
import com.goyeau.moulin.util.getCanonicalScalaName

/** Fully qualified name for a def or val in it's final object instance. Matches the way you would call the def or val.
  */
opaque type Name <: String = String

object Name:
  inline given Name = ${ nameMacro }

  private def nameMacro(using Quotes): Expr[Name] =
    import quotes.reflect.*

    def enclosingsUntilClassDef(sym: Symbol): Seq[Symbol] =
      if sym.isClassDef then Seq(sym)
      else if (sym.isDefDef || sym.isValDef) && !sym.isLocalDummy && !sym.flags.is(Flags.Synthetic) &&
        !sym.flags.is(Flags.Macro) && !sym.name.contains("$proxy")
      then enclosingsUntilClassDef(sym.maybeOwner) :+ sym
      else enclosingsUntilClassDef(sym.maybeOwner)

    // Get the enclosing items until the class, trait or object
    val enclosings = enclosingsUntilClassDef(Symbol.spliceOwner)
    // report.warning("a: " + enclosings.last.flags.show)

    // Get the first item that should be the class, trait or object and get the name from the Class
    val `this`     = This(enclosings.head).asExpr
    val objectName = '{ ${ `this` }.getClass.getCanonicalScalaName }

    // Get the rest of the items that should be defs and vals and get the name from the Symbol
    val defOrValNameMaybe = enclosings.tail.map(_.name).mkString(".")
    val defOrValName      = Expr(if defOrValNameMaybe.nonEmpty then s".$defOrValNameMaybe" else "")

    '{ ${ objectName } + ${ defOrValName } }
