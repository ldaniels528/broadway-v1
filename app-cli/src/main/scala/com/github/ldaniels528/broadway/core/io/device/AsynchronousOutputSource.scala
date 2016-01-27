package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.{ExecutionContext, Future}

/**
  * Asynchronous Output Source
  */
trait AsynchronousOutputSource extends OutputSource {

  def allWritesCompleted(scope: Scope)(implicit ec: ExecutionContext): Future[Unit]

}
