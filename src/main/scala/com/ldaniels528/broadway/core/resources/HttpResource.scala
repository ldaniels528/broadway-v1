package com.ldaniels528.broadway.core.resources

import java.io.InputStream
import java.net.URL

/**
 * Represents a HTTP GET/POST resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class HttpResource(url: String) extends ReadableResource {

  override def getResourceName = None

  override def getInputStream: Option[InputStream] = Option(new URL(url).openConnection().getInputStream)

  override def toString = s"http:$url"

}