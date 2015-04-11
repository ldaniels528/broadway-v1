package com.ldaniels528.broadway.server.http

import com.ldaniels528.broadway.core.narrative.Anthology
import com.ldaniels528.broadway.core.util.PropertiesHelper._
import com.ldaniels528.broadway.server.ServerConfig
import com.ldaniels528.broadway.server.http.ServerContext.NarrativeJs

/**
 * Broadway Server Context
 * @author lawrence.daniels@gmail.com
 */
class ServerContext(val config: ServerConfig,
                    val anthologies: Seq[Anthology]) {

  lazy val narratives = anthologies.flatMap(_.narratives) map (n => NarrativeJs(n.id, n.className, n.properties.toMap))

}

/**
 * Broadway Server Context Singleton
 * @author lawrence.daniels@gmail.com
 */
object ServerContext {

  case class NarrativeJs(id: String, className: String, properties: Map[String, String])

}