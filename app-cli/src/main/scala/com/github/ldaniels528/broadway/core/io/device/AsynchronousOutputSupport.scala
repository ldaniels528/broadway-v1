package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope

import scala.concurrent.{ExecutionContext, Future}

/**
  * Asynchronous Output Support Capability
  * @author lawrence.daniels@gmail.com
  */
trait AsynchronousOutputSupport {
  self: OutputSource =>

  def allWritesCompleted(implicit scope: Scope, ec: ExecutionContext): Future[OutputSource]

}
