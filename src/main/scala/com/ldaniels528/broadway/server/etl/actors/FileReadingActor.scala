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
    case TextCopy(resource, target) => textCopy(target, resource, None)
    case TextParse(resource, handler, target) => textCopy(target, resource, Option(handler))
    case message => unhandled(message)
  }

  /**
   * Copies the contents of a binary file
   * @param target the given target actor
   * @param resource the resource to read from
   */
  private def binaryCopy(target: ActorRef, resource: ReadableResource) {
    val buf = new Array[Byte](1024)
    resource.getInputStream foreach { in =>
      val count = in.read(buf)
      if (count == buf.length) target ! buf
      else if (count > 0) {
        val bytes = new Array[Byte](count)
        System.arraycopy(buf, 0, bytes, 0, count)
        target ! bytes
      }
      else if (count == -1) {
        target ! ClosingFile(resource)
      }
    }
  }

  /**
   * Copies the contents of an ASCII file
   * @param target the given target actor
   * @param resource the resource to read from
   * @param formatHandler the optional text handler
   */
  private def textCopy(target: ActorRef, resource: ReadableResource, formatHandler: Option[TextFormatHandler]) {
    // notify the target actor that the resource has been opened
    target ! OpeningFile(resource)

    // transmit all the lines of the file
    var lineNo = 0
    resource.getInputStream foreach { in =>
      Source.fromInputStream(in).getLines() foreach { line =>
        lineNo += 1
        formatHandler match {
          case Some(handler) => target ! TextLine(lineNo, line, handler.parse(line))
          case None => target ! TextLine(lineNo, line)
        }
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

  case class TextCopy(resource: ReadableResource, target: ActorRef)

  case class TextParse(resource: ReadableResource, handler: TextFormatHandler, target: ActorRef)

  case class TextLine(lineNo: Long, line: String, tokens: Seq[String] = Nil)

  /**
   * Base class for all text format handlers
   */
  trait TextFormatHandler {

    def parse(line: String): Array[String]

  }

  /**
   * Comma Separated Values (CSV) format handler
   */
  case object CSV extends TextFormatHandler {

    override def parse(line: String): Array[String] = {
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
      (if (sb.nonEmpty) sb.toString :: list else list).reverse.toArray
    }

  }

  /**
   * Delimited text format handler
   * @param delimiter the given delimiter character or sequence
   */
  case class Delimited(delimiter: String) extends TextFormatHandler {
    private val splitter = s"[$delimiter]"

    override def parse(line: String): Array[String] = line.split(splitter)
  }

  case class OpeningFile(readableResource: ReadableResource)

  /**
   * This message is sent once the actor has reach the end-of-file for the given resource
   * @param readableResource the given [[ReadableResource]]
   */
  case class ClosingFile(readableResource: ReadableResource)

}