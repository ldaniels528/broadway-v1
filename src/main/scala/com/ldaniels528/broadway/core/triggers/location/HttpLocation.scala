package com.ldaniels528.broadway.core.triggers.location

import com.ldaniels528.broadway.core.narrative.{Feed, FeedDescriptor, NarrativeRuntime}

/**
 * HTTP Location
 * @param id the given unique identifier
 * @param path the given remote URL (e.g. "http://www.otcbb.com/dailylist/txthistory/")
 * @param feeds the "configured" feeds
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class HttpLocation(id: String, path: String, feeds: Seq[FeedDescriptor]) extends Location {
  /**
   * Attempts to find a feed that corresponds to the given feed name
   * @param name the given feed name
   * @param rt the implicit topology runtime
   * @return an option of a [[Feed]]
   */
  override def findFeed(name: String)(implicit rt: NarrativeRuntime): Option[Feed] = {
    feeds.find(_.matches(name)) map (_.toFeed)
  }

}
