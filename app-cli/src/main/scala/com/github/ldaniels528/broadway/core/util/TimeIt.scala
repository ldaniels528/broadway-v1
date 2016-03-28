package com.github.ldaniels528.broadway.core.util

import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

/**
  * Time It
  * @author lawrence.daniels@gmail.com
  */
object TimeIt {
  private val logger = LoggerFactory.getLogger(getClass)
  private val first = new AtomicBoolean(true)

  def once[T](block: => T): Option[T] = {
    if(first.compareAndSet(true, false)) Option(block) else None
  }

  def time[T](label: String)(block: => T): T = {
    val startTime = System.currentTimeMillis()
    val result = block
    val elapsedTime = System.currentTimeMillis() - startTime
    logger.info(s"$label in $elapsedTime msec")
    result
  }

}
