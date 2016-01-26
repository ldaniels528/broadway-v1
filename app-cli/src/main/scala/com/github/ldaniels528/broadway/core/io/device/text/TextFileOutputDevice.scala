package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedWriter, FileWriter}

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.{ExecutionContext, Future}

/**
  * Text File Output Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileOutputDevice(id: String, path: String) extends OutputDevice with StatisticsGeneration {

  var offset = 0L

  override def open(scope: Scope) = {
    scope.openWriter(new BufferedWriter(new FileWriter(scope.evaluate(path))))
  }

  override def close(scope: Scope)(implicit ec: ExecutionContext) = {
    Future.successful(scope.getWriter[BufferedWriter].close())
  }

  override def write(scope: Scope, data: Data) = {
    val writer = scope.getWriter[BufferedWriter]
    writer.write(data.asText)
    writer.newLine()
    offset += updateCount(1)
    1
  }

}
