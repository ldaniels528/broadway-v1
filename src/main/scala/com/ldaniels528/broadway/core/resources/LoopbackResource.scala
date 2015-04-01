package com.ldaniels528.broadway.core.resources

import java.io.InputStream

/**
 * Represents a loopback resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class LoopbackResource(name: String) extends ReadableResource {

  override def getResourceName: Option[String] = Option(name)

  override def getInputStream: Option[InputStream] = None

}
