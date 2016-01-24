package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.RuntimeContext

import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a Generic Input or Output Device
  */
trait Device {

  def id: String

  def offset: Long

  def count: Long

  def open(rt: RuntimeContext): Unit

  def close(rt: RuntimeContext)(implicit ec: ExecutionContext): Future[Unit]

}
