package com.github.ldaniels528.broadway.core.io.layout

/**
  * Represents a Field
  */
case class Field(name: String, `type`: String = "string", value: Option[String] = None, length: Option[Int] = None)