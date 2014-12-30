package com.ldaniels528.broadway.server.etl.actors

import akka.actor.{Actor, ActorRef}
import com.ldaniels528.broadway.core.Resources.ReadableResource
import com.ldaniels528.broadway.server.etl.actors.TextFileReader.{DelimitedFile, TextFile}
import org.slf4j.LoggerFactory

import scala.io.Source

/**
 * This actor is capable of reading/parsing text file
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class TextFileReader() extends Actor {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  override def receive = {
    case TextFile(resource: ReadableResource, target) => copyFile(target, resource)
    case DelimitedFile(resource, delimiter, target) => copyFile(target, resource, Some(s"[$delimiter]"))
    case message => unhandled(message)
  }

  private def copyFile(target: ActorRef, resource: ReadableResource, delimiter: Option[String] = None) = {
    logger.info(s"Reading from '$resource'...")
    resource.getInputStream foreach { in =>
      Source.fromInputStream(in).getLines() foreach { line =>
        delimiter.map { splitter =>
          target ! line.split(splitter)
        } getOrElse {
          target ! line
        }
      }
    }
  }

}

/**
 * Text File Reader Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object TextFileReader {

  case class CSVFile(resource: ReadableResource, target: ActorRef)

  case class DelimitedFile(resource: ReadableResource, delimiter: String, target: ActorRef)

  case class TextFile(resource: ReadableResource, target: ActorRef)

}