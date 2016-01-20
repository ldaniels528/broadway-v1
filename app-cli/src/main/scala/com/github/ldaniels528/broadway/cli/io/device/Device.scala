package com.github.ldaniels528.broadway.cli.io.device

import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a Generic Input or Output Device
  */
trait Device {

  def id: String

  def count: Long

  def open(): Unit

  def close()(implicit ec: ExecutionContext): Future[Unit]

}
