package com.ldaniels528.broadway.core.resources

import java.io.InputStream

/**
 * Represents a readable resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait ReadableResource extends Resource {

  /**
   * Returns an option of an input stream
   * @return an option of an input stream
   */
  def getInputStream: Option[InputStream]

}