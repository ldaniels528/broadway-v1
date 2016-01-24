package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedWriter, FileWriter}

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{DataWriting, OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.text.TextLayout
import com.ldaniels528.commons.helpers.OptionHelper.Risky._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * Text Output Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextOutputDevice(id: String, path: String, layout: TextLayout)
  extends OutputDevice with DataWriting with StatisticsGeneration with TextWriting {

  private val logger = LoggerFactory.getLogger(getClass)
  private var writer: Option[BufferedWriter] = None

  var offset = 0L

  override def open(rt: RuntimeContext) = {
    writer match {
      case Some(device) =>
        logger.warn(s"Device '$path' is already open")
      case None =>
        writer = new BufferedWriter(new FileWriter(path))
    }
  }

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
    Future.successful(writer.foreach(_.close()))
  }

  override def writeLine(line: String) = {
    writer.foreach { w =>
      w.write(line)
      w.newLine()
      offset += updateCount(1)
    }
    1
  }

  override def write(data: Data) = {
    writer.foreach { w =>
      w.write(data.asText)
      w.newLine()
      offset += updateCount(1)
    }
    1
  }

}
