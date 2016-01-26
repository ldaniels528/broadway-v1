package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.cli.actors.TaskActorPool
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.scope.Scope
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Multiple Device Output Source
  */
case class MultiOutputDevice(id: String, devices: Seq[OutputDevice], concurrency: Int)
  extends OutputDevice with StatisticsGeneration {

  private val logger = LoggerFactory.getLogger(getClass)
  private val taskActorPool = new TaskActorPool(concurrency)

  override def close(scope: Scope)(implicit ec: ExecutionContext) = {
    for {
      _ <- taskActorPool.die(1.hour)
      _ <- {
        logger.info("Closing devices...")
        Future.sequence(devices.map(_.close(scope)))
      }
    } yield ()
  }

  override def open(scope: Scope) = devices.foreach(_.open(scope))

  override def offset = devices.map(_.offset).max

  override def write(scope: Scope, data: Data) = {
    taskActorPool ! new Runnable {
      override def run() {
        updateCount(devices.foldLeft[Int](0) { (total, device) =>
          total + device.write(scope, data)
        })
      }
    }
    devices.length
  }

}