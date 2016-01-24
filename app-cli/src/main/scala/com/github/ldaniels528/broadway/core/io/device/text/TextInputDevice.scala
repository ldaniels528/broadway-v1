package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedReader, FileReader}

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.device.{InputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.text.TextLayout
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Text Input Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextInputDevice(val id: String, path: String, layout: TextLayout)
  extends InputDevice with StatisticsGeneration with TextReading {

  private var reader: Option[BufferedReader] = None

  var offset = 0L

  override def open(rt: RuntimeContext) = {
    reader = new BufferedReader(new FileReader(path))
  }

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
    Future.successful(reader.foreach(_.close()))
  }

  override def readLine = {
    val line = reader.flatMap(r => Option(r.readLine))
    offset += updateCount(1)
    line
  }

}
