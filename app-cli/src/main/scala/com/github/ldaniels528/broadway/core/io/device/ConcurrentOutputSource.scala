package com.github.ldaniels528.broadway.core.io.device

import java.util.concurrent.Callable

import akka.util.Timeout
import com.github.ldaniels528.broadway.core.actors.TaskActorPool
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Concurrent Output Source
  */
case class ConcurrentOutputSource(id: String, devices: Seq[OutputSource], concurrency: Int) extends AsynchronousOutputSource {
  private val taskActorPool = new TaskActorPool(concurrency)
  private var ticker = 0

  override def allWritesCompleted(scope: Scope)(implicit ec: ExecutionContext) = {
    taskActorPool.die(1.hour) map (_ => ())
  }

  override def close(scope: Scope) {
    devices.foreach(_.close(scope))
  }

  override def open(scope: Scope) = {
    scope ++= Seq(
      "flow.output.concurrency" -> concurrency,
      "flow.output.devices" -> devices.length
    )
    devices.foreach(_.open(scope))
  }

  override def write(scope: Scope, data: Data) = {
    implicit val timeout: Timeout = 30.seconds
    ticker += 1
    val promise = taskActorPool ? new Callable[Int] {
      override def call: Int = devices(ticker % devices.length).write(scope, data)
    }
    promise foreach (updateCount(scope, _))
    0
  }

}
