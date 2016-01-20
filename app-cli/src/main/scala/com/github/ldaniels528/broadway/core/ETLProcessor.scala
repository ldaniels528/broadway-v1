package com.github.ldaniels528.broadway.core

import java.io.File

import com.github.ldaniels528.broadway.core.io.device.{OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.util.TimeIt._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Extract Transform and Loading (ETL) Processor
  *
  * @author lawrence.daniels@gmail.com
  */
class ETLProcessor() {
  private val logger = LoggerFactory.getLogger(getClass)

  def load(configFile: File): Option[ETLConfig] = {
    logger.info(s"Loading ETL config '${configFile.getAbsolutePath}'...")
    XMLConfigParser(configFile).parse
  }

  def run(configFile: File) {
    load(configFile) match {
      case Some(config) => run(RuntimeContext(config))
      case None =>
        throw new IllegalArgumentException(s"ETL configuration file '${configFile.getName}' is invalid")
    }
  }

  /**
    * Executes the ETL processing
    *
    * @param rt the given [[RuntimeContext runtime context]]
    */
  def run(rt: RuntimeContext) {
    logger.info(s"Loaded ETL config '${rt.id}'...")
    val devices = rt.devices

    // open the output devices
    logger.info("Opening the input and output devices...")
    devices.foreach(_.open(rt))

    // execute the ETL process
    logger.info(s"Running ETL config '${rt.id}'...")
    time("Process completed") {
      rt.flows.foreach(_.execute(rt))
    }

    // close the output devices
    logger.info("Closing the input and output devices...")
    val promiseToClose = Future.sequence(devices.map(_.close(rt)))

    // display the statistics
    promiseToClose onComplete {
      case Success(_) => showStatistics(rt)
      case Failure(e) =>
        logger.error("Processing failed", e)
    }

    Thread.sleep(1.seconds.toMillis)
  }

  private def showStatistics(rt: RuntimeContext) = {
    logger.info("-" * 60)
    rt.flows.foreach { flow =>
      val stats = flow match {
        case d: StatisticsGeneration =>
          f"- records: ${d.count}, ${d.avgRecordsPerSecond}%.1f records/sec"
        case _ => ""
      }
      logger.info(f"flow: ${flow.id} $stats")

      flow.devices.sortBy(_.id).foreach { device =>
        val deviceType = if (device.isInstanceOf[OutputDevice]) "written" else "read"
        val stats = device match {
          case d: StatisticsGeneration => f"- ${d.avgRecordsPerSecond}%.1f records/sec"
          case _ => ""
        }

        logger.info(s" \t device: ${device.id}: ${device.count} $deviceType $stats")
      }
    }
    logger.info("-" * 60)
  }

}
