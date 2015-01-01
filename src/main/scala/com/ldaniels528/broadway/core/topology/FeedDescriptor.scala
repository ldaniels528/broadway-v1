package com.ldaniels528.broadway.core.topology

import java.util.UUID

/**
 * Represents a descriptor for a feed that will be realized when a match file is encountered
 * @param name the name of the feed
 * @param matching the feed matching strategy
 * @param dependencies the given feed dependencies
 * @param topology the topology to execute
 */
case class FeedDescriptor(name: String, matching: String, dependencies: Seq[FeedDescriptor] = Nil, topology: Option[Topology] = None) {
  val uuid = UUID.randomUUID().toString

  /**
   * Indicates whether the given feed name is a match for this feed
   * @param feedName the given feed name
   * @return true, the given feed name is a match for this feed
   */
  def matches(feedName: String): Boolean = {
    matching match {
      case "exact" => name == feedName
      case "regex" => name.matches(feedName)
      case "start" => name.startsWith(feedName)
      case "ends" => name.endsWith(feedName)
      case _ => name == feedName
    }
  }

  def toFeed(implicit rt: TopologyRuntime): Feed = {
    val feed = Feed(uuid, name, dependencies map (_.toFeed), topology)
    rt.feeds.getOrElseUpdate(feed.uuid, feed)
  }

}
