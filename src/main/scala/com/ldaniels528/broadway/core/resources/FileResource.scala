package com.ldaniels528.broadway.core.resources

import java.io.{File, FileInputStream, InputStream}

/**
 * Represents a local file system resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class FileResource(path: String) extends ReadableResource {

  override def getResourceName = Option(new File(path)) map(_.getName)
  
  override def getInputStream: Option[InputStream] = Option(new FileInputStream(path))

  override def toString = s"file:$path"

}
