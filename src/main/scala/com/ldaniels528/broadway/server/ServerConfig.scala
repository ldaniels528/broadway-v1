package com.ldaniels528.broadway.server

import java.io.File

import akka.actor.{Actor, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.ldaniels528.broadway.core.actors.{ArchivingActor, NarrativeProcessingActor}
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.util.FileHelper._
import com.ldaniels528.broadway.server.ServerConfig._
import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.util.PropertiesHelper._

import scala.reflect.ClassTag

/**
 * Server Config
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class ServerConfig(props: java.util.Properties, httpInfo: Option[HttpInfo]) {
  implicit val system = ActorSystem(props.getOrElse("broadway.actor.system", "BroadwaySystem"))

  // create the system actors
  lazy val archivingActor = prepareActor(new ArchivingActor(this))
  lazy val processingActor = prepareActor(new NarrativeProcessingActor(this), parallelism = 10)

  /**
   * Prepares a new actor for execution within the narrative
   * @param actor the given [[Actor]]
   * @param parallelism the number of actors to create
   * @tparam T the actor type
   * @return an [[akka.actor.ActorRef]]
   */
  def prepareActor[T <: Actor : ClassTag](actor: => T, parallelism: Int = 1) = {
    system.actorOf(Props(actor).withRouter(RoundRobinPool(nrOfInstances = parallelism)))
  }

  def getRootDirectory = new File(props.asOpt[String](BaseDir).orDie(s"Required property '$BaseDir' is missing"))

  def getAnthologiesDirectory = new File(getRootDirectory, "anthologies")

  def getArchiveDirectory = new File(getRootDirectory, "archive")

  def getCompletedDirectory = new File(getRootDirectory, "completed")

  def getFailedDirectory = new File(getRootDirectory, "failed")

  def getIncomingDirectory = new File(getRootDirectory, "incoming")

  def getWorkDirectory = new File(getRootDirectory, "work")

  /**
   * Initializes the environment based on this configuration
   */
  def init() = Seq(
    getArchiveDirectory, getCompletedDirectory, getFailedDirectory,
    getIncomingDirectory, getAnthologiesDirectory, getWorkDirectory) foreach ensureExistence

}

/**
 * Server Config Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ServerConfig {
  private val BaseDir = "broadway.directories.base"

  /**
   * Loads the default server configuration
   * @return the default server configuration
   */
  def apply(): Option[ServerConfig] = apply(ClasspathResource("/broadway-config.xml"))

  /**
   * Loads the server configuration from the given resource
   * @return the server configuration
   */
  def apply(resource: ReadableResource): Option[ServerConfig] = ServerConfigParser.parse(resource)

  case class HttpInfo(host: String, port: Int)

}