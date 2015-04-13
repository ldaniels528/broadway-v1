package com.ldaniels528.broadway.core.actors

import com.ldaniels528.broadway.BroadwayNarrative
import com.ldaniels528.broadway.core.actors.NarrativeProcessingActor.RunJob
import com.ldaniels528.broadway.core.resources.Resource
import com.ldaniels528.broadway.server.ServerConfig

/**
 * This is an internal use actor that is responsible for processing narratives
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class NarrativeProcessingActor(config: ServerConfig) extends BroadwayActor {
  override def receive = {
    case RunJob(narrative, resource) => narrative.start(resource)
    case message => unhandled(message)
  }
}

/**
 * Narrative Processing Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object NarrativeProcessingActor {

  /**
   * This message causes the the given narrative to be invoked; consuming the given resource
   * @param narrative the given [[BroadwayNarrative]]
   * @param resource the given [[Resource]]
   */
  case class RunJob(narrative: BroadwayNarrative, resource: Option[Resource])

}
