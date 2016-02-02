package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.actors.{BroadwayActorSystem, TaskActorPool}
import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.layout.Record
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Concurrent Output Source
  */
case class ConcurrentOutputSource(id: String, concurrency: Int, devices: Seq[OutputSource])
  extends AsynchronousOutputSource {

  private val taskActorPool = new TaskActorPool(concurrency)
  private var ticker = 0
  private var lastWrite = 0L

  override def allWritesCompleted(implicit scope: Scope, ec: ExecutionContext) = {
    taskActorPool.die(4.hours) map (_ => ())
  }

  override def close(scope: Scope) = delayedClose(scope)

  override def layout = {
    devices.headOption.map(_.layout) orDie "No output sources were specified"
  }

  override def open(scope: Scope) = {
    scope ++= Seq(
      "flow.output.id" -> id,
      "flow.output.concurrency" -> concurrency,
      "flow.output.devices" -> devices.length
    )
    devices.foreach(_.open(scope))
  }

  override def writeRecord(record: Record)(implicit scope: Scope) = {
    0
  }

  private def delayedClose(scope: Scope) {
    BroadwayActorSystem.system.scheduler.scheduleOnce(delay = 15.minutes) {
      if (System.currentTimeMillis() - lastWrite >= 15.minutes.toMillis) devices.foreach(_.close(scope))
      else delayedClose(scope)
    }
    ()
  }


}
