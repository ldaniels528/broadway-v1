package com.github.ldaniels528.broadway.core.util

import org.slf4j.LoggerFactory

/**
  * Time It
  *
  * @author lawrence.daniels@gmail.com
  */
object TimeIt {
  private val logger = LoggerFactory.getLogger(getClass)

  def time[T](label: String)(block: => T): T = {
    val startTime = System.currentTimeMillis()
   val result = block
    val elaspedTime = System.currentTimeMillis() - startTime
    logger.info(s"$label in $elaspedTime msec")
    result
  }

}
