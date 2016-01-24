package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.cli.actors.TaskActorPool
import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.device.text.TextWriting
import com.ldaniels528.commons.helpers.OptionHelper._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Round Robin Output Device
  */
object RoundRobinOutputDevice {

  def apply(id: String, devices: Seq[OutputDevice], concurrency: Int) = {
    devices.headOption orDie "No output devices" match {
      case device: BinaryWriting => BinaryWritingRoundRobinOutputDevice(id, devices.map(_.asInstanceOf[OutputDevice with BinaryWriting]), concurrency)
      case device: TextWriting => TextWritingRoundRobinOutputDevice(id, devices.map(_.asInstanceOf[OutputDevice with TextWriting]), concurrency)
      case device =>
        throw new IllegalArgumentException(s"Unsupported device type '${device.id}'")
    }
  }

  /**
    * Text Writing Round Robin Output Device
    */
  case class TextWritingRoundRobinOutputDevice(id: String, devices: Seq[OutputDevice with TextWriting], concurrency: Int)
    extends OutputDevice with TextWriting with StatisticsGeneration {

    private val logger = LoggerFactory.getLogger(getClass)
    private val taskActorPool = new TaskActorPool(concurrency)
    private var ticker = 0

    override def layout = devices(ticker % devices.length).layout

    override def offset = devices(ticker % devices.length).offset

    override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
      for {
        _ <- taskActorPool.die(1.hour)
        _ <- {
          logger.info("Closing devices...")
          Future.sequence(devices.map(_.close(rt)))
        }
      } yield ()
    }

    override def open(rt: RuntimeContext) = devices.foreach(_.open(rt))

    override def writeLine(line: String) = {
      ticker += 1
      taskActorPool ! new Runnable {
        override def run() {
          updateCount(devices(ticker % devices.length).writeLine(line))
          ()
        }
      }
      0
    }
  }

  /**
    * Binary Writing Round Robin Output Device
    */
  case class BinaryWritingRoundRobinOutputDevice(id: String, devices: Seq[OutputDevice with BinaryWriting], concurrency: Int)
    extends OutputDevice with BinaryWriting with StatisticsGeneration {

    private val logger = LoggerFactory.getLogger(getClass)
    private val taskActorPool = new TaskActorPool(concurrency)
    private var ticker = 0

    override def layout = devices(ticker % devices.length).layout

    override def offset = devices(ticker % devices.length).offset

    override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
      for {
        _ <- taskActorPool.die(1.hour)
        _ <- {
          logger.info("Closing devices...")
          Future.sequence(devices.map(_.close(rt)))
        }
      } yield ()
    }

    override def open(rt: RuntimeContext) = devices.foreach(_.open(rt))

    override def writeBytes(bytes: Array[Byte]) = {
      ticker += 1
      taskActorPool ! new Runnable {
        override def run() {
          updateCount(devices(ticker % devices.length).writeBytes(bytes))
          ()
        }
      }
      0
    }
  }

}
