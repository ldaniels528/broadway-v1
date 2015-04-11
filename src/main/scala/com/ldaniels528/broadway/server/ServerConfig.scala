package com.ldaniels528.broadway.server

import java.io.{FilenameFilter, File}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.ldaniels528.broadway.core.actors.file.ArchivingActor
import com.ldaniels528.broadway.core.narrative.{AnthologyParser, Anthology}
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.util.FileHelper._
import com.ldaniels528.broadway.server.ServerConfig._
import com.ldaniels528.broadway.server.http.ServerContext
import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.util.PropertiesHelper._
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

/**
 * Server Config
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class ServerConfig(props: java.util.Properties, httpInfo: Option[HttpInfo]) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  implicit val system = ActorSystem(props.getOrElse("broadway.actor.system", "BroadwaySystem"))
  private val actorCache = TrieMap[Class[_ <: Actor], ActorRef]()

  // create the system actors
  lazy val archivingActor = prepareActor(new ArchivingActor(this))

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
  def init() = {
    // ensure all directories exist
    Seq(
      getArchiveDirectory, getCompletedDirectory, getFailedDirectory,
      getIncomingDirectory, getAnthologiesDirectory, getWorkDirectory) foreach ensureExistence

    // load the anthologies
    val anthologies = loadAnthologies(getAnthologiesDirectory)

    // return the server context
    new ServerContext(this, anthologies)
  }

  /**
   * Loads all anthologies from the given directory
   * @param directory the given directory
   * @return the collection of successfully parsed [[Anthology]] objects
   */
  private def loadAnthologies(directory: File): Seq[Anthology] = {
    logger.info(s"Searching for narrative configuration files in '${directory.getAbsolutePath}'...")
    val xmlFile = directory.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.toLowerCase.endsWith(".xml")
    })
    xmlFile.toSeq flatMap (f => AnthologyParser.parse(FileResource(f.getAbsolutePath)))
  }

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