package com.github.ldaniels528.broadway.core.io.flow

import com.github.ldaniels528.broadway.core.io.OpCode
import com.github.ldaniels528.broadway.core.io.device.IOSource

import scala.concurrent.Future

/**
  * ETL Process Flow
  *
  * @author lawrence.daniels@gmail.com
  */
trait Flow extends OpCode[Future[Unit]] {

  def id: String

  def devices: Seq[IOSource]

}
