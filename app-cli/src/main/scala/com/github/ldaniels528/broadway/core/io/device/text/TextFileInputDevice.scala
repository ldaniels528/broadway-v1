package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedReader, FileReader}

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.text.fields.TextLineFieldSet
import com.github.ldaniels528.broadway.core.scope.Scope
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Text File Input Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileInputDevice(val id: String, path: String) extends InputDevice with StatisticsGeneration {
  private val fieldSet = TextLineFieldSet("line")

  var offset = 0L

  override def open(scope: Scope) = {
    scope.openReader(new BufferedReader(new FileReader(scope.evaluate(path))))
  }

  override def close(scope: Scope)(implicit ec: ExecutionContext) = {
    Future.successful(scope.getReader[BufferedReader].close())
  }

  override def read(scope: Scope): Option[Data] = {
    val reader = scope.getReader[BufferedReader]
    val data = reader.flatMap(r => Option(r.readLine)).map(Data(fieldSet, _))
    offset += updateCount(1)
    data
  }

}
