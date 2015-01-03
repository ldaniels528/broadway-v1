package com.ldaniels528.broadway.core.resources

import java.io.{File, InputStream}

/**
 * Represents a class path-based file resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class ClasspathResource(path: String) extends ReadableResource {

  override def getResourceName = Option(new File(path)) map(_.getName)

  override def getInputStream: Option[InputStream] = Option(getClass.getResource(path)).map(_.openStream())

  override def toString = s"classpath:$path"

}