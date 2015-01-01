package com.ldaniels528.broadway.core.topology

/**
 * Represents a location; a container for incoming feeds
 * @param feeds the "configured" feeds
 */
case class Location(feeds: Seq[FeedDescriptor]) {

  def findFeed(name: String)(implicit rt: TopologyRuntime): Option[Feed] = feeds.find(_.matches(name)) map (_.toFeed)

}
