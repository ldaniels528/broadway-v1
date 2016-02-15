package com.github.ldaniels528.broadway.app.config

import java.io.File

import com.github.ldaniels528.commons.helpers.OptionHelper._

import scala.language.postfixOps
import scala.util.Try
import scala.xml.{Node, XML}

/**
  * Server Configuration Parser
  *
  * @author lawrence.daniels@gmail.com
  */
object ServerConfigParser {

  /**
    * Attempts to extract a server configuration from the given resource
    *
    * @param file the given configuration [[File file]]
    * @return a [[ServerConfig]]
    */
  def parse(file: File): Try[ServerConfig] = {
    for {
      doc <- Try(XML.loadFile(file))
      directories <- parseDirectories(doc)
    } yield ServerConfig(directories)
  }

  /**
    * Parses the <directories base="...">...</directories> tag
    *
    * @param root the given XML node
    * @return a [[java.util.Properties]] object containing the specified properties
    */
  private def parseDirectories(root: Node) = Try {
    (root \ "directories" map { node =>
      Directories(new File(node \@ "base"))
    } headOption) orDie "Only one directories element is allowed"
  }

}

