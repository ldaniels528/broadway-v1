package com.ldaniels528.broadway.core.topology

import java.io.File

import com.ldaniels528.broadway.core.resources._

/**
 * Represents a location; a container for incoming feeds
 * @param feeds the "configured" feeds
 */
case class Location(id: String, path: String, feeds: Seq[FeedDescriptor]) {

  /**
   * Attempts to find a feed that corresponds to the given feed name
   * @param name the given feed name
   * @param rt the implicit topology runtime
   * @return an option of a [[Feed]]
   */
  def findFeed(name: String)(implicit rt: TopologyRuntime): Option[Feed] = {
    feeds.find(_.matches(name)) map (_.toFeed)
  }

  def toResource: ReadableResource = FileResource(path)

  def toFile: Option[File] = Some(new File(path))

}
