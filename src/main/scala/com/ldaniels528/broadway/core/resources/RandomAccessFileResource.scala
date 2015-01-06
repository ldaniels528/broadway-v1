package com.ldaniels528.broadway.core.resources

import java.io.{File, RandomAccessFile}

import scala.collection.concurrent.TrieMap

/**
 * Random Access File Resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class RandomAccessFileResource(val path: String) {
  private val file = new File(path).getCanonicalFile
  private val raf = new RandomAccessFile(file, "rw")

  def read(limit: Int) = {
    val buf = new Array[Byte](limit)
    val count = raf.read(buf)
    (buf, count)
  }

  /**
   * Writes the given bytes to the underlying file at the given offset
   * @param offset the given offset
   * @param bytes the given array of bytes
   */
  def write(offset: Long, bytes: Array[Byte]) {
    raf.seek(offset)
    raf.write(bytes)
  }

  /**
   * Writes a line of text to the underlying file
   * @param line the given line of text
   */
  def write(line: String) = raf.writeChars(line)

}

/**
 * Random Access File Resource Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object RandomAccessFileResource {
  private val resources = TrieMap[File, RandomAccessFileResource]()

  def apply(path: String): RandomAccessFileResource = {
    val file = new File(path).getCanonicalFile
    resources.getOrElseUpdate(file, new RandomAccessFileResource(path))
  }

  def unapply(resource: RandomAccessFileResource) = Option(resource.path)

}
