package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedReader, FileReader}

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.device.{InputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.InputLayout
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Text Input Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextInputDevice(val id: String, path: String, layout: InputLayout)
  extends InputDevice with StatisticsGeneration {

  private var reader: Option[BufferedReader] = None
  private var offset = 0L
  private var eof = false

  override def open(rt: RuntimeContext) = {
    reader = new BufferedReader(new FileReader(path))
    eof = false
  }

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
    Future.successful(reader.foreach(_.close()))
  }

  override def hasNext = !eof

  override def read() = {
    for {
      device <- reader
      line <- {
        val line_? = Option(device.readLine())
        eof = line_?.isEmpty
        offset += 1
        if (!eof) updateCount(1)
        line_?
      }
      outcome <- layout.decode(offset, line)

    } yield outcome
  }

}
