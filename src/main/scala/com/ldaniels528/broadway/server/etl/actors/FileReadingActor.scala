package com.ldaniels528.broadway.server.etl.actors

import akka.actor.{Actor, ActorRef}
import com.ldaniels528.broadway.core.Resources.ReadableResource
import com.ldaniels528.broadway.server.etl.actors.FileReadingActor._

import scala.io.Source

/**
 * This actor is capable of reading/parsing binary/text files
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class FileReadingActor() extends Actor {

  override def receive = {
    case BinaryCopy(resource, target) => binaryCopy(target, resource)
    case TextCopy(resource, target, handler) => textCopy(target, resource, handler)
    case message => unhandled(message)
  }

  /**
   * Copies the contents of a binary file
   * @param target the given target actor
   * @param resource the resource to read from
   */
  private def binaryCopy(target: ActorRef, resource: ReadableResource) {
    // use 64K blocks
    val buf = new Array[Byte](65536)
    resource.getInputStream foreach { in =>
      // notify the target actor that the resource has been opened
      target ! OpeningFile(resource)

      // transmit the blocks
      var offset = 0L
      val count = in.read(buf)
      if (count == -1) target ! ClosingFile(resource)
      else {
        target ! BinaryBlock(offset, copyBlock(buf, count))
      }
      offset += count
    }
  }

  /**
   * Copies the data from the given buffer and returns a new buffer containing the data
   * @param buf the given buffer
   * @param count the given number of bytes
   * @return the new byte array containing a copy of the data
   */
  private def copyBlock(buf: Array[Byte], count: Int) = {
    if (count == buf.length) buf
    else {
      val bytes = new Array[Byte](count)
      System.arraycopy(buf, 0, bytes, 0, count)
      bytes
    }
  }

  /**
   * Copies the contents of an ASCII file
   * @param target the given target actor
   * @param resource the resource to read from
   * @param formatHandler the optional text handler
   */
  private def textCopy(target: ActorRef, resource: ReadableResource, formatHandler: Option[TextFormatHandler]) {
    var lineNo = 1
    resource.getInputStream foreach { in =>
      // notify the target actor that the resource has been opened
      target ! OpeningFile(resource)

      // transmit all the lines of the file
      Source.fromInputStream(in).getLines() foreach { line =>
        target ! TextLine(lineNo, line, formatHandler.map(_.parse(line)) getOrElse Nil)
        lineNo += 1
      }

      // notify the target actor that the resource has been closed
      target ! ClosingFile(resource)
    }
  }

}

/**
 * File Reader Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object FileReadingActor {

  case class BinaryCopy(resource: ReadableResource, target: ActorRef)

  case class BinaryBlock(offset: Long, data: Array[Byte])

  /**
   * This message is sent once the actor has reach the end-of-file for the given resource
   * @param readableResource the given [[ReadableResource]]
   */
  case class ClosingFile(readableResource: ReadableResource)

  /**
   * This message is sent when the given resource is opened for reading
   * @param readableResource the given [[ReadableResource]]
   */
  case class OpeningFile(readableResource: ReadableResource)

  case class TextCopy(resource: ReadableResource, target: ActorRef, handler: Option[TextFormatHandler] = None)

  case class TextLine(lineNo: Long, line: String, tokens: List[String] = Nil)

  /**
   * Base class for all text format handlers
   */
  trait TextFormatHandler {

    def parse(line: String): List[String]

  }

  /**
   * Comma Separated Values (CSV) format handler
   */
  case object CSV extends TextFormatHandler {

    override def parse(line: String) = {
      val sb = new StringBuilder()
      var inQuotes = false

      // extract the tokens
      val list = line.foldLeft[List[String]](Nil) { (list, ch) =>
        val result: Option[String] = ch match {
          // quoted text
          case '"' =>
            inQuotes = !inQuotes
            None

          // comma (unquoted)?
          case c if c == ',' && !inQuotes =>
            if (sb.nonEmpty) {
              val s = sb.toString()
              sb.clear()
              Option(s)
            } else None

          // any other character
          case c =>
            sb += c
            None
        }

        result map (_ :: list) getOrElse list
      }

      // add the last token
      (if (sb.nonEmpty) sb.toString :: list else list).reverse
    }

  }

  /**
   * Delimited text format handler
   * @param splitter the given delimiter regular expression (e.g. "[,]")
   */
  case class Delimited(splitter: String) extends TextFormatHandler {
    override def parse(line: String) = line.split(splitter).toList
  }

}