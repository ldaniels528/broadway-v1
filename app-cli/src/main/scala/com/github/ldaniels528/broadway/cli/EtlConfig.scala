package com.github.ldaniels528.broadway.cli

import com.github.ldaniels528.broadway.cli.flow.Flow
import org.slf4j.LoggerFactory

/**
  * ETL Configuration
  *
  * @author lawrence.daniels@gmail.com
  */
case class EtlConfig(id: String, flows: Seq[Flow]) {
  private val logger = LoggerFactory.getLogger(getClass)

  def findDevice(id: String) = flows.flatMap(_.devices).find(_.id == id)

  def evaluate(value: String) = {
    val sb = new StringBuilder(value)
    var lastIndex = -1
    do {
      val start = sb.indexOf("{{", lastIndex)
      val end = sb.indexOf("}}", start)
      if (start != -1 && end > start) {
        val reference = sb.substring(start + 2, end - 1).trim
        val result = evaluateExpression(reference).getOrElse("????")
        logger.info(s"expr = [$reference], result = [$result], replace = [${sb.substring(start, end + 2)}]")
        sb.replace(start, end + 2, result)
        lastIndex = start
      }
      else lastIndex = -1

    } while (lastIndex != -1 && lastIndex < value.length)

    sb.toString()
  }

  private def evaluateExpression(expr: String) = {
    // is it a data source reference? (e.g. "input_file.__OFFSET")
    if (expr.contains('.')) {
      val (op1, op2) = expr.splitAt(expr.indexOf('.'))

      // lookup the device
      findDevice(op1) flatMap { device =>
        op2.tail match {
          case "__OFFSET" => Some(String.valueOf(device.count))
          case "__IO" => Some(String.valueOf(device.count))
          case variable =>
            throw new IllegalArgumentException(s"Unrecognized property '$variable'")
        }
      }
    }
    else None
  }

}
