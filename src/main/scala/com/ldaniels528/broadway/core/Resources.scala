package com.ldaniels528.broadway.core

import java.io.{FileInputStream, InputStream}
import java.net.URL

/**
 * Broadway Resource Classes
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object Resources {

  /**
   * Represents a readable resource
   */
  trait ReadableResource extends Serializable {
    def getInputStream: Option[InputStream]
  }

  case class FileResource(path: String) extends ReadableResource {
    override def getInputStream: Option[InputStream] = Option(new FileInputStream(path))
  }

  case class HttpResource(url: String) extends ReadableResource {
    override def getInputStream: Option[InputStream] = Option(new URL(url).openConnection().getInputStream)
  }

  case class ClasspathResource(path: String) extends ReadableResource {
    override def getInputStream: Option[InputStream] = Option(getClass.getResource(path)).map(_.openStream())
  }

}
