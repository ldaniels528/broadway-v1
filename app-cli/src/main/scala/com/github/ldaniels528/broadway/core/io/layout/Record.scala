package com.github.ldaniels528.broadway.core.io.layout

import scala.reflect.ClassTag

/**
  * Represents a generic data record
  */
trait Record {

  def id: String

  def fields: Seq[Field]

  override def toString = s"${getClass.getSimpleName}(${fields.map(f => s"${f.name}=...").mkString(", ")})"

}

/**
  * Record Companion Object
  */
object Record {

  implicit class RecordEnrichment(val record: Record) extends AnyVal {

    def promote[T <: Record](implicit tag: ClassTag[T]) = record match {
      case rec: T => Option(rec)
      case _ => None
    }

  }

}