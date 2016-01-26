package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.scope.Scope
import com.github.ldaniels528.broadway.core.scope.Scope.ScopeFunction
import com.ldaniels528.commons.helpers.OptionHelper._

/**
  * ETL Expression Compiler
  */
object ETLCompiler {

  def compile(scope: Scope, expr: String): ScopeFunction = {
    // is it a data source reference? (e.g. "input_file.__OFFSET")
    if (expr.contains('.')) {
      val (name, property) = expr.splitAt(expr.indexOf('.'))
      scope.find(name, property.tail) orDie s"Unrecognized property '$expr'"
    }
    else {
      throw new IllegalArgumentException(s"Syntax error '$expr'")
    }
  }

  def handlebars(scope: Scope, expr: String): String = {
    val sb = new StringBuilder(expr)
    var lastIndex = -1
    do {
      val start = sb.indexOf("{{", lastIndex)
      val end = sb.indexOf("}}", start)
      if (start != -1 && end > start) {
        val reference = sb.substring(start + 2, end - 1).trim
        sb.replace(start, end + 2, unwrap(compile(scope, reference)(scope)))
        lastIndex = start
      }
      else lastIndex = -1

    } while (lastIndex != -1 && lastIndex < sb.length)

    sb.toString()
  }

  private def unwrap(result: Any): String = {
    result match {
      case o: Option[_] => unwrap(o)
      case value => value.toString
    }
  }

}
