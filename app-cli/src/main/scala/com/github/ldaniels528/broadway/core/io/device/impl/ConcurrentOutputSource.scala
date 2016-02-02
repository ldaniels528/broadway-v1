package com.github.ldaniels528.broadway.core.io.device.impl

import java.util.concurrent.Callable

import akka.util.Timeout
import com.github.ldaniels528.broadway.core.actors.{BroadwayActorSystem, TaskActorPool}
import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.impl.ConcurrentOutputSource._
import com.github.ldaniels528.broadway.core.io.device.{AsynchronousOutputSupport, OutputSource}
import com.github.ldaniels528.broadway.core.io.record.Record
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.implicitConversions

/**
  * Concurrent Output Source
  */
case class ConcurrentOutputSource(id: String, concurrency: Int, devices: Seq[OutputSource])
  extends OutputSource with AsynchronousOutputSupport {

  private val taskActorPool = new TaskActorPool(concurrency)
  private var ticker = 0
  private var lastWrite = 0L

  override def allWritesCompleted(implicit scope: Scope, ec: ExecutionContext) = {
    taskActorPool.die(4.hours) map (_ => this)
  }

  override def close(implicit scope: Scope) = delayedClose(scope)

  override def layout = devices.headOption.map(_.layout) orDie "No output sources were specified"

  override def open(implicit scope: Scope) = {
    scope ++= Seq(
      "flow.output.id" -> id,
      "flow.output.concurrency" -> concurrency,
      "flow.output.devices" -> devices.length
    )
    devices.foreach(_.open(scope))
  }

  override def writeRecord(record: Record)(implicit scope: Scope) = {
    implicit val timeout: Timeout = 30.seconds
    lastWrite = System.currentTimeMillis()
    ticker += 1
    val promise = taskActorPool ? (() => devices(ticker % devices.length).writeRecord(record))
    promise foreach updateCount
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

/**
  * Concurrent Output Source Companion Object
  */
object ConcurrentOutputSource {

  implicit def function2Callable[T](f: () => T): Callable[T] = new Callable[T] {
    override def call = f()
  }

}