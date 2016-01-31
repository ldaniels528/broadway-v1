package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.layout.DataTypes.DataType

/**
  * Represents a generic column, field, property or XML element
  *
  * @param name       the name of the element
  * @param `type`     the data type of the element
  * @param value      the value of the element
  * @param properties the properties or attributes of the element
  * @param elements   the child elements of this element
  */
case class Field(name: String,
                 `type`: DataType = DataTypes.STRING,
                 var value: Option[Any] = None,
                 length: Option[Int] = None,
                 properties: Seq[Field] = Nil,
                 elements: Seq[Field] = Nil)
