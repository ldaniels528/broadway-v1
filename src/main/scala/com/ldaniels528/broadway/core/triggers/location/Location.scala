package com.ldaniels528.broadway.core.triggers.location

import com.ldaniels528.broadway.core.narrative.{FeedDescriptor, Feed, NarrativeRuntime}

/**
 * Represents a location; an abstract container for incoming feeds
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait Location {

  /**
   * Returns the location's unique identifier
   * @return the unique identifier
   */
  def id: String

  /**
   * Returns the configured feed descriptors
   * @return a collection of [[FeedDescriptor]]
   */
  def feeds: Seq[FeedDescriptor]

  /**
   * Attempts to find a feed that corresponds to the given feed name
   * @param name the given feed name
   * @param rt the implicit topology runtime
   * @return an option of a [[Feed]]
   */
  def findFeed(name: String)(implicit rt: NarrativeRuntime): Option[Feed]

}
