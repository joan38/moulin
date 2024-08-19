package com.goyeau.moulin.command

import com.goyeau.moulin.command.WildCard.Modules
import scala.reflect.ClassTag
import scala.util.Try
import scala.Dynamic
import scala.language.dynamics

trait WildCard:
  def any: Modules[Any]                        = WildCard.any(this)
  def anyOf[Module: ClassTag]: Modules[Module] = WildCard.anyOf(this)[Module]

  def all                                  = WildCard.all(this)
  def allOf[Module: ClassTag]: Seq[Module] = WildCard.allOf(this)[Module]

/** Provides the `all` and `any` to run functions on all available objects on first level or recursively.
  */
object WildCard:
  extension [T](parent: T)
    def any: Modules[Any] = Modules(anyOf[Any])
    def anyOf[Module: ClassTag]: Modules[Module] = Modules(
      parent.getClass.getFields
        // Get fields that are of type T
        .filter(field => getAllExtendedTypes(field.getType()).contains(summon[ClassTag[Module]].runtimeClass))
        // Get the value of the field
        .map(_.getType.getField("MODULE$").get(null).asInstanceOf[Module])
    )

    def all: Modules[Any] = Modules(allOf[Any])
    def allOf[Module: ClassTag]: Modules[Module] =
      Modules(anyOf[Module].flatMap(_.anyOf[Module]))

  class Modules[+Module](modules: Seq[Module]) extends Seq[Module] with Dynamic:
    def selectDynamic(name: String): Seq[Any] = applyDynamic(name)()
    def applyDynamic(name: String)(args: Any*): Seq[Any] =
      val results = modules.flatMap(invoke(_, name, args))
      if results.isEmpty
      then throw IllegalArgumentException(s"$name not found")
      else results

    override def apply(i: Int): Module      = modules.apply(i)
    override def iterator: Iterator[Module] = modules.iterator
    override def length: Int                = modules.length

  private def invoke(parent: Any, name: String, args: Seq[Any]): Option[Any] =
    val methods = parent.getClass.getMethods
      .filter(method => method.getParameterCount == args.size && method.getName == name)
      .map(_.invoke(parent, args*))
    val fields = parent.getClass.getFields
      .filter(_.getName == name)
      .map(f => Try(f.getType.getField("MODULE$").get(null)).getOrElse(f.get(parent)))
    (methods ++ fields).headOption

  private def getAllExtendedTypes(clazz: Class[?]): Array[Class[?]] =
    if clazz != null
    then
      (clazz.getSuperclass +: clazz.getInterfaces)
        .flatMap(extendedType => extendedType +: getAllExtendedTypes(extendedType))
    else Array.empty
