package com.github.ldaniels528.broadway.core.io.device

import java.util.concurrent.Callable

import akka.util.Timeout
import com.github.ldaniels528.broadway.cli.actors.TaskActorPool
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.scope.Scope
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
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

  implicit val timeout: Timeout = 30.seconds

  override def close(scope: Scope)(implicit ec: ExecutionContext) = {
    for {
      _ <- taskActorPool.die(1.hour)
      _ <- {
        logger.info("Closing devices...")
        Future.sequence(devices.map(_.close(scope)))
      }
    } yield ()
  }

  override def offset = devices(ticker).offset

  override def open(scope: Scope) = devices.foreach(_.open(scope))

  override def write(scope: Scope, data: Data) = {
    ticker += 1
    val promise = taskActorPool ? new Callable[Int] {
      override def call: Int = devices(ticker % devices.length).write(scope, data)
    }
    promise foreach updateCount
    0
  }

}
