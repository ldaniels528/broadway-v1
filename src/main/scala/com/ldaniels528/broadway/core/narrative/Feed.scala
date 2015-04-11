package com.ldaniels528.broadway.core.narrative

/**
 * Represents an actual file feed
 * @param name the name of the feed
 * @param dependencies the given feed dependencies
 * @param descriptor the topology to execute
 */
case class Feed(uuid: String, name: String, dependencies: Seq[Feed], descriptor: Option[NarrativeDescriptor]) {
  var processed: Boolean = false

  /**
   * Indicates whether the feed is ready to be executed
   * @return true, if no dependencies exist or all dependencies have been satisfied
   */
  def ready: Boolean = dependencies.isEmpty || dependencies.forall(_.processed)

}
