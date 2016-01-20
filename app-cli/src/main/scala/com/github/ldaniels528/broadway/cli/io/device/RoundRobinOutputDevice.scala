package com.github.ldaniels528.broadway.cli.io.device

import com.ldaniels528.commons.helpers.OptionHelper._
import com.github.ldaniels528.broadway.cli.actors.TaskActorPool
import com.github.ldaniels528.broadway.cli.io.Data
import com.github.ldaniels528.broadway.cli.io.layout.OutputLayout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Round Robin Output Device
  */
case class RoundRobinOutputDevice(id: String, devices: Seq[OutputDevice], concurrency: Int) extends OutputDevice with StatisticsGeneration {
  private val logger = LoggerFactory.getLogger(getClass)
  private val taskActorPool = new TaskActorPool(concurrency)
  private var ticker = -1

  override def layout: OutputLayout = devices.headOption.map(_.layout).orDie("No layout found")

  override def close()(implicit ec: ExecutionContext) = {
    for {
      _ <- taskActorPool.die(1.hour)
      _ <- {
        logger.info("Closing devices...")
        Future.sequence(devices.map(_.close()))
      }
    } yield ()
  }

  override def open() = devices.foreach(_.open())

  override def write(data: Data) = {
    ticker += 1
    taskActorPool ! new Runnable {
      override def run() {
        updateCount(devices(ticker % devices.length).write(data))
        ()
      }
    }
    1
  }

}
