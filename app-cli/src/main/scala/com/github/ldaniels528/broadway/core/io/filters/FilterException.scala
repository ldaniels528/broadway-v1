package com.github.ldaniels528.broadway.core.io.filters

/**
  * Filter Exception
  */
class FilterException(filter: Filter, message: String)
  extends RuntimeException(s"${filter.getClass.getSimpleName}: $message")