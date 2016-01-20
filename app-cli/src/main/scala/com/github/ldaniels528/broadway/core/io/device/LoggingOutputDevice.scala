package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.layout.OutputLayout
import com.github.ldaniels528.broadway.core.RuntimeContext
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * Logging Output Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class LoggingOutputDevice(id: String, layout: OutputLayout) extends OutputDevice with StatisticsGeneration {
  private val logger = LoggerFactory.getLogger(getClass)

  override def open(rt: RuntimeContext) {}

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = Future.successful({})

  override def write(data: Data) = {
    logger.info(data.toString)
    updateCount(1)
  }

}