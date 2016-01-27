package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.actors.TaskActorPool
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Pooled Output Source
  */
case class PooledOutputSource(id: String, devices: Seq[OutputSource], concurrency: Int) extends AsynchronousOutputSource {
  private val taskActorPool = new TaskActorPool(concurrency)

  override def allWritesCompleted(scope: Scope)(implicit ec: ExecutionContext) = {
    taskActorPool.die(1.hour) map (_ => ())
  }

  override def close(scope: Scope) = {
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
    taskActorPool ! new Runnable {
      override def run() {
        updateCount(scope, devices.foldLeft[Int](0) { (total, device) =>
          total + device.write(scope, data)
        })
      }
    }
    devices.length
  }

}