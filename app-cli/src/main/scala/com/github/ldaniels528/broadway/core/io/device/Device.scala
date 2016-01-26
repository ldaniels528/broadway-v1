package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a Generic Input or Output Device
  */
trait Device {

  def id: String

  def offset: Long

  def count: Long

  def open(scope: Scope): Unit

  def close(scope: Scope)(implicit ec: ExecutionContext): Future[Unit]

}
