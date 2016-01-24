package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.cli.actors.TaskActorPool
import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Round Robin Output Device
  */
case class RoundRobinOutputDevice(id: String, devices: Seq[OutputDevice], concurrency: Int)
  extends OutputDevice with StatisticsGeneration {

  private val logger = LoggerFactory.getLogger(getClass)
  private val taskActorPool = new TaskActorPool(concurrency)
  private var ticker = 0

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
    for {
      _ <- taskActorPool.die(1.hour)
      _ <- {
        logger.info("Closing devices...")
        Future.sequence(devices.map(_.close(rt)))
      }
    } yield ()
  }

  override def offset = devices(ticker).offset

  override def open(rt: RuntimeContext) = devices.foreach(_.open(rt))

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
