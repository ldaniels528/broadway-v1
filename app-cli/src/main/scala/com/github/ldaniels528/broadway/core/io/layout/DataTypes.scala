package com.github.ldaniels528.broadway.core.io.layout

/**
  * Data Types
  */
object DataTypes extends Enumeration {
  type DataType = Value

  val BOOLEAN, DATE, DOUBLE, FLOAT, INT, LONG, STRING = Value

  /**
    * Data Type Enrichment
    *
    * @param dataType the given [[DataType data type]]
    */
  implicit class DataTypeEnrichment(val dataType: DataType) extends AnyVal {

    def toTypeName = dataType.toString.toLowerCase
  }

}
