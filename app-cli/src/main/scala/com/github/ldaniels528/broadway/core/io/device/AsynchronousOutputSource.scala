package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope

import scala.concurrent.{ExecutionContext, Future}

/**
  * Asynchronous Output Source
  */
trait AsynchronousOutputSource extends OutputSource {

  def allWritesCompleted(implicit scope: Scope, ec: ExecutionContext): Future[Unit]

}
