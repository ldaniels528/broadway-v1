package com.ldaniels528.broadway.core.narrative

import java.util.UUID

/**
 * Represents a descriptor for a feed that will be realized when a match file is encountered
 * @param name the name of the feed
 * @param matching the feed matching strategy
 * @param dependencies the given feed dependencies
 * @param narrative the narrative to execute
 */
case class FeedDescriptor(name: String,
                          matching: String,
                          dependencies: Seq[FeedDescriptor] = Nil,
                          narrative: Option[NarrativeDescriptor] = None) {
  // get the unique ID
  val uuid = UUID.randomUUID().toString

  /**
   * Indicates whether the given feed name is a match for this feed
   * @param feedName the given feed name
   * @return true, the given feed name is a match for this feed
   */
  def matches(feedName: String): Boolean = {
    matching match {
      case "exact" => feedName.toLowerCase == name.toLowerCase
      case "regex" => feedName.toLowerCase.matches(name.toLowerCase)
      case "start" => feedName.toLowerCase.startsWith(name.toLowerCase)
      case "ends" => feedName.toLowerCase.endsWith(name.toLowerCase)
      case unhanded =>
        throw new IllegalArgumentException(s"Feed match type '$unhanded' was not recognized")
    }
  }

  def toFeed(implicit rt: NarrativeRuntime): Feed = rt.getFeed(this)

}
