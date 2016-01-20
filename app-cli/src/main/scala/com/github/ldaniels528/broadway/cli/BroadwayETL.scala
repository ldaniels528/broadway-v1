package com.github.ldaniels528.broadway.cli

import java.io.{FileNotFoundException, File}

import com.github.ldaniels528.broadway.cli.io.device.{OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.cli.util.TimeIt._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Broadway: Extract Transform and Loading (ETL) Utility
  *
  * @author lawrence.daniels@gmail.com
  */
object BroadwayETL {
  private val Version = "0.10"
  private val logger = LoggerFactory.getLogger(getClass)

  /**
    * For standalone execution
    *
    * @param args the given command line arguments
    */
  def main(args: Array[String]) {
    logger.info(s"Broadway ETL v$Version")
    if (args.isEmpty) {
      throw new IllegalArgumentException(s"${getClass.getSimpleName} <etlConfig.xml>")
    }

    val configFile = new File(args(0))
    if(!configFile.exists()) {
      throw new FileNotFoundException(configFile.getAbsolutePath)
    }

    logger.info(s"Loading ETL config '${configFile.getAbsolutePath}'...")
    EtlConfigParser(configFile).parse match {
      case Some(config) => run(config)
      case None =>
        throw new IllegalArgumentException(s"ETL configuration file '${configFile.getName}' is invalid")
    }
  }

  /**
    * Executes the ETL processing
    *
    * @param etlConfig the given [[EtlConfig ETL configuration]]
    */
  def run(etlConfig: EtlConfig) {
    logger.info(s"Loaded ETL config '${etlConfig.id}'...")
    val devices = etlConfig.flows.flatMap(_.devices).distinct

    // open the output devices
    logger.info("Opening the input and output devices...")
    devices.foreach(_.open())

    // execute the ETL process
    logger.info(s"Running ETL config '${etlConfig.id}'...")
    time("Process completed") {
      etlConfig.flows.foreach(_.execute(etlConfig))
    }

    // close the output devices
    logger.info("Closing the input and output devices...")
    val promiseToClose = Future.sequence(devices.map(_.close()))

    // display the statistics
    promiseToClose onComplete {
      case Success(_) => showStatistics(etlConfig)
      case Failure(e) =>
        logger.error("Processing failed", e)
    }

    Thread.sleep(1.seconds.toMillis)
  }

  private def showStatistics(etlConfig: EtlConfig) = {
    logger.info("-" * 60)
    etlConfig.flows.foreach { flow =>
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
