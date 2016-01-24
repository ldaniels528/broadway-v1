package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedReader, FileReader}

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.text.fields.TextLineFieldSet
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Text Input Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextInputDevice(val id: String, path: String) extends InputDevice with StatisticsGeneration {
  private var reader: Option[BufferedReader] = None
  private val fieldSet = TextLineFieldSet("line")

  var offset = 0L

  override def open(rt: RuntimeContext) = {
    reader = new BufferedReader(new FileReader(path))
  }

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
    Future.successful(reader.foreach(_.close()))
  }

  override def read(): Option[Data] = {
    val data = reader.flatMap(r => Option(r.readLine)).map(Data(fieldSet, _))
    offset += updateCount(1)
    data
  }

}
