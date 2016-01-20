package com.github.ldaniels528.broadway.cli.io.device.text

import java.io.{BufferedWriter, FileWriter}

import com.github.ldaniels528.broadway.cli.io.Data
import com.github.ldaniels528.broadway.cli.io.device.{OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.cli.io.layout.OutputLayout
import com.ldaniels528.commons.helpers.OptionHelper.Risky._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * Text Output Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextOutputDevice(id: String, path: String, layout: OutputLayout) extends OutputDevice with StatisticsGeneration {
  private val logger = LoggerFactory.getLogger(getClass)
  private var writer: Option[BufferedWriter] = None
  private var offset = 0L

  override def open() = {
    writer match {
      case Some(device) =>
        logger.warn(s"Device '$path' is already open")
      case None =>
        writer = new BufferedWriter(new FileWriter(path))
    }
  }

  override def close()(implicit ec: ExecutionContext) = {
    Future.successful(writer.foreach(_.close()))
  }

  override def write(data: Data) = {
    (for {
      device <- writer
      _ = offset += 1
      formattedData <- layout.encode(offset, data)
    } yield {
      device.write(formattedData)
      device.newLine()
      updateCount(1)
    }) getOrElse 0
  }

}
