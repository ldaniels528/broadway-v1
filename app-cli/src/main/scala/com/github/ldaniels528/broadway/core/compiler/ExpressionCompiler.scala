package com.github.ldaniels528.broadway.core.compiler

import com.github.ldaniels528.broadway.core.io.Scope

import scala.util.{Failure, Success, Try}

/**
  * ETL Expression Compiler
  * @author lawrence.daniels@gmail.com
  */
object ExpressionCompiler {

  def handlebars(scope: Scope, expr: String) = {
    val sb = new StringBuilder(expr)
    var lastIndex = -1
    do {
      val start = sb.indexOf("{{", lastIndex)
      val end = sb.indexOf("}}", start)
      if (start != -1 && end > start) {
        val reference = sb.substring(start + 2, end - 1).trim
        sb.replace(start, end + 2, unwrap(scope.find(reference)))
        lastIndex = start
      }
      else lastIndex = -1

    } while (lastIndex != -1 && lastIndex < sb.length)

    sb.toString()
  }

  def unwrap(result: Any): String = {
    result match {
      case e: Either[_, _] => e match {
        case Left(value) => unwrap(value)
        case Right(value) => unwrap(value)
      }
      case o: Option[_] => o match {
        case Some(value) => unwrap(value)
        case None => ""
      }
      case t: Try[_] => t match {
        case Success(value) => unwrap(value)
        case Failure(e) => e.getMessage
      }
      case value => value.toString
    }
  }

}
