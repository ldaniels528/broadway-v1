package com.ldaniels528.broadway.core.resources

import java.io._

import scala.io.Source

/**
 * Represents a local text file system resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class TextFileResource(path: String) extends IterableResource[String] {
  private val file = new File(path)

  override def getResourceName = Option(file) map (_.getName)

  override def iterator: Iterator[String] = Source.fromFile(file).getLines()

  override def toString = s"file:$path"

}
