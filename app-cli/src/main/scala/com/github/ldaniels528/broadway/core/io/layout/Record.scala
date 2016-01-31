package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.layout.RecordTypes._
import com.github.ldaniels528.broadway.core.scope.{InheritedScope, Scope}

/**
  * Represents a generic data record
  */
trait Record {

  def id: String

  def duplicate: Record

  def fields: Seq[Field]

  def `type`: RecordType

  def copyAs(outputTemplate: Record)(implicit parentScope: Scope) = {
    // populate the scope with the input record's values
    val scope = InheritedScope(parentScope)
    populate(scope)

    // create and populate the output record
    val outputRec = outputTemplate.duplicate
    outputRec.fields zip fields foreach { case (out, in) =>
      out.value = in.value map {
        case expr: String if expr.contains("{{") => scope.evaluate(expr)
        case value => value
      }
    }
    outputRec
  }

  def populate(scope: Scope) {
    scope ++= {
      for {
        field <- fields
        value <- field.value
      } yield s"$id.${field.name}" -> value
    }
  }

  override def toString = {
    s"${getClass.getSimpleName}(${fields.map(f => s"""${f.name}="${f.value.getOrElse("")}"""").mkString(", ")})"
  }

}
