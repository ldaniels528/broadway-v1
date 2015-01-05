package com.ldaniels528.broadway.server

import java.io.File

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.ldaniels528.broadway.core.actors.Actors._
import com.ldaniels528.broadway.core.actors.ArchivingActor
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
  implicit val system = ActorSystem("BroadwaySystem")

  // create the system actors
  val archivingActor = addActor(new ArchivingActor(this))

  /**
   * Adds a new actor to the narrative
   * @param parallelism the number of actors to create
   * @tparam T the actor type
   * @return an [[akka.actor.ActorRef]]
   */
  def addActor[T <: Actor : ClassTag](parallelism: Int = 1): BWxActorRef = {
    actorClosure((1 to parallelism) map (_ => system.actorOf(Props[T]())))
  }

  /**
   * Adds a new actor to the narrative
   * @param actor the given [[Actor]]
   * @param parallelism the number of actors to create
   * @tparam T the actor type
   * @return an [[akka.actor.ActorRef]]
   */
  def addActor[T <: Actor : ClassTag](actor: => T, parallelism: Int = 1): BWxActorRef = {
    actorClosure((1 to parallelism) map (_ => system.actorOf(Props(actor))))
  }

  /**
   * Returns a closure to a function that exposes an actor (round robin) from the actor pool
   * @param actors the given pool of [[ActorRef]]
   * @return a closure to a function that exposes an actor (round robin) from the actor pool
   */
  private def actorClosure(actors: Seq[ActorRef]) = {
    var index = 0
    () => {
      index += 1
      actors(index % actors.length)
    }
  }

  def getRootDirectory = new File(props.asOpt[String](BaseDir).orDie(s"Required property '$BaseDir' is missing"))

  def getArchiveDirectory = new File(getRootDirectory, "archive")

  def getCompletedDirectory = new File(getRootDirectory, "completed")

  def getFailedDirectory = new File(getRootDirectory, "failed")

  def getIncomingDirectory = new File(getRootDirectory, "incoming")

  def getTopologiesDirectory = new File(getRootDirectory, "topologies")

  def getWorkDirectory = new File(getRootDirectory, "work")

  /**
   * Initializes the environment based on this configuration
   */
  def init() = Seq(
    getArchiveDirectory, getCompletedDirectory, getFailedDirectory,
    getIncomingDirectory, getTopologiesDirectory, getWorkDirectory) foreach ensureExistence

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