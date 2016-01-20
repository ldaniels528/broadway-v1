package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.opcode._

/**
  * ETL Expression Compiler
  */
object ETLCompiler {

  def compile(rt: RuntimeContext, expr: String): Option[OpCode] = {
    // is it a data source reference? (e.g. "input_file.__OFFSET")
    if (expr.contains('.')) {
      val (op1, op2) = expr.splitAt(expr.indexOf('.'))

      // lookup the device
      rt.devices.find(_.id == op1) flatMap { device =>
        op2.tail match {
          case "__OFFSET" => Some(new DeviceOffsetOpCode(device))
          case "__IO" => Some(new DeviceIOCountOpCode(device))
          case variable =>
            throw new IllegalArgumentException(s"Unrecognized property '$variable'")
        }
      }
    }
    else None
  }

  def handlebars(rt: RuntimeContext, expr: String) = {
    val sb = new StringBuilder(expr)
    var lastIndex = -1
    do {
      val start = sb.indexOf("{{", lastIndex)
      val end = sb.indexOf("}}", start)
      if (start != -1 && end > start) {
        val reference = sb.substring(start + 2, end - 1).trim
        val result = compile(rt, reference).flatMap(_.eval(rt)).map(_.toString).getOrElse("????")
        sb.replace(start, end + 2, result)
        lastIndex = start
      }
      else lastIndex = -1

    } while (lastIndex != -1 && lastIndex < sb.length)

    sb.toString()
  }

}
