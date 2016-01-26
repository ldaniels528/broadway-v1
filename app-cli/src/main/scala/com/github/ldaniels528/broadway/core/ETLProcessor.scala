package com.github.ldaniels528.broadway.core

import java.io.File

import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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
      case Some(config) => run(config)
      case None =>
        throw new IllegalArgumentException(s"ETL configuration file '${configFile.getName}' is invalid")
    }
  }

  /**
    * Executes the ETL processing
    *
    * @param config the given [[ETLConfig ETL configuration]]
    */
  def run(config: ETLConfig) {
    logger.info(s"Executing story '${config.id}'...")
    config.triggers foreach { trigger =>
      trigger.execute(config)
    }

    Thread.sleep(1.seconds.toMillis)
    logger.info("*" * 30 + " PROCESS COMPLETED " + "*" * 30)
  }

}
