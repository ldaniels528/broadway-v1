package com.ldaniels528.broadway.core.resources

import java.io.InputStream

/**
 * Represents a readable resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait ReadableResource extends Serializable {

  def getResourceName: Option[String]

  def getInputStream: Option[InputStream]

}