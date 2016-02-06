package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * Represents an Input Source
  * @author lawrence.daniels@gmail.com
  */
trait InputSource extends DataSource

/**
  * Input Source Companion Object
  * @author lawrence.daniels@gmail.com
  */
object InputSource {

  /**
    * Input Source Enrichment Utilities
    *
    * @param source the given [[InputSource input source]]
    */
  implicit class InputSourceEnrichment(val source: InputSource) extends AnyVal {

    def readText(implicit scope: Scope) = {
      source match {
        case device: TextReadingSupport => device.readText
        case device =>
          throw new IllegalArgumentException(s"Input source '${device.id}' is not a text-capable device")
      }
    }

  }

}