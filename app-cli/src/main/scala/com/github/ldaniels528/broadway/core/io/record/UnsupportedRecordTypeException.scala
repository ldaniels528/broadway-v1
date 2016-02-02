package com.github.ldaniels528.broadway.core.io.record

/**
  * Unsupported Record Type Exception
  */
class UnsupportedRecordTypeException(record: Record)
  extends RuntimeException(s"Unsupported record type '${record.getClass.getSimpleName}'")