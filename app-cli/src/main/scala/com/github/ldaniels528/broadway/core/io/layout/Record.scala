package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.layout.DataTypes.DataType
import com.github.ldaniels528.broadway.core.io.layout.Record.Element
import com.github.ldaniels528.broadway.core.io.layout.RecordTypes._

/**
  * Represents a generic data record
  */
trait Record {

  def fields: Seq[Element]

  def `type`: RecordType

}

/**
  * Record Companion Object
  */
object Record {

  /**
    * Represents a generic column, field, property or XML element
    * @param name the name of the element
    * @param `type` the data type of the element
    * @param value the value of the element
    * @param properties the properties or attributes of the element
    * @param elements the child elements of this element
    */
  case class Element(name: String,
                     `type`: DataType,
                     var value: Option[Any] = None,
                     length: Option[Int] = None,
                     properties: Seq[Element] = Nil,
                     elements: Seq[Element] = Nil)

}