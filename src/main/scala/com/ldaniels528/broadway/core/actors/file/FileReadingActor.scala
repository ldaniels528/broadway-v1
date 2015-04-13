package com.ldaniels528.broadway.core.actors.file

import akka.actor.ActorRef
import com.ldaniels528.broadway.core.actors.BroadwayActor
import com.ldaniels528.broadway.core.actors.file.FileReadingActor._
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.util.TextFileHelper
import com.ldaniels528.broadway.server.ServerConfig

import scala.io.Source
import scala.language.implicitConversions

/**
 * This actor is capable of reading/parsing binary/text files
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class FileReadingActor(config: ServerConfig) extends BroadwayActor {
  override def receive = {
    case CopyBinary(resource, target) => copyBinary(target, resource)
    case CopyText(resource, target, handler) => copyText(target, resource, handler)
    case TransformFile(resource, target, transform) => transformFile(resource, target, transform)
    case message => unhandled(message)
  }

  /**
   * Copies the contents of a binary file
   * @param target the given target actor
   * @param resource the resource to read from
   */
  private def copyBinary(target: ActorRef, resource: ReadableResource) {
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
        target ! BinaryBlock(resource, offset, copyBlock(buf, count))
      }
      offset += count
    }

    // archive the resource
    config.archivingActor ! resource
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
  private def copyText(target: ActorRef, resource: ReadableResource, formatHandler: Option[TextFormatHandler]) {
    var lineNo = 1
    resource.getInputStream foreach { in =>
      // notify the target actor that the resource has been opened
      target ! OpeningFile(resource)

      // transmit all the lines of the file
      Source.fromInputStream(in).getLines() foreach { line =>
        target ! TextLine(resource, lineNo, line, formatHandler.map(_.parse(line)) getOrElse Nil)
        lineNo += 1
      }

      // notify the target actor that the resource has been closed
      target ! ClosingFile(resource)

      // archive the resource
      config.archivingActor ! resource
    }
  }

  /**
   * Processes the given resource, while performing the given transformation on each record.
   * @param target the given target actor
   * @param resource the resource to read from
   * @param transform the given transformation function
   */
  private def transformFile[T](resource: ReadableResource, target: ActorRef, transform: (Long, String) => Option[T]) {
    var lineNo = 1L
    resource.getInputStream foreach { in =>
      // notify the target actor that the resource has been opened
      target ! OpeningFile(resource)

      // transmit all the lines of the file
      Source.fromInputStream(in).getLines() foreach { line =>
        transform(lineNo, line) foreach (target ! _)
        lineNo += 1
      }

      // notify the target actor that the resource has been closed
      target ! ClosingFile(resource)

      // archive the resource
      config.archivingActor ! resource
    }
  }


}

/**
 * File Reader Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object FileReadingActor {

  /**
   * Implicit conversion for using text format handler instances instead of an option of a text format handler
   * @param handler the given text format handler
   * @return an option of a text format handler when a text format handler is supplied
   */
  implicit def formatHandler2Option(handler: TextFormatHandler): Option[TextFormatHandler] = Option(handler)

  /**
   * Represents a block of binary data
   * @param resource the given [[ReadableResource]]
   * @param offset the given offset within the file
   * @param data the given block of binary data
   */
  case class BinaryBlock(resource: ReadableResource, offset: Long, data: Array[Byte])

  /**
   * This message is sent once the actor has reach the end-of-file for the given resource
   * @param resource the given [[ReadableResource]]
   */
  case class ClosingFile(resource: ReadableResource)

  /**
   * This message initiates a process to copy the contents of a file (as binary) to the given target actor
   * @param resource the resource representing the content
   * @param target the given target [[ActorRef]]
   */
  case class CopyBinary(resource: ReadableResource, target: ActorRef)

  /**
   * This message initiates a process to copy the contents of a file (as ASCII) to the given target actor
   * @param resource the resource representing the content
   * @param target the given target [[ActorRef]]
   * @param handler the optional text format handler
   * @see CSV
   * @see Delimited
   */
  case class CopyText(resource: ReadableResource, target: ActorRef, handler: Option[TextFormatHandler] = None)

  /**
   * This message is sent when the given resource is opened for reading
   * @param resource the given [[ReadableResource]]
   */
  case class OpeningFile(resource: ReadableResource)

  /**
   * Processes the given resource, while performing the given transformation on each record.
   * @param resource the given resource
   * @param target the given target [[ActorRef]]
   * @param transform the given transform
   */
  case class TransformFile[T](resource: ReadableResource, target: ActorRef, transform: (Long, String) => Option[T])

  /**
   * Represents a line of text read from the given resource (and optionally parsed into tokens)
   * @param resource the given [[ReadableResource]]
   * @param lineNo the given line number
   * @param line the given line of text
   * @param tokens the optional tokens
   */
  case class TextLine(resource: ReadableResource, lineNo: Long, line: String, tokens: List[String] = Nil)

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

    override def parse(line: String) = TextFileHelper.parseCSV(line)

  }

  /**
   * Delimited text format handler
   * @param splitter the given delimiter regular expression (e.g. "[,]")
   */
  case class Delimited(splitter: String) extends TextFormatHandler {

    override def parse(line: String) = TextFileHelper.parseTokens(line, splitter)

  }

}