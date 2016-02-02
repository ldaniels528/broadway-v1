package com.github.ldaniels528.broadway.core.io.trigger

import java.util.Date

import com.github.ldaniels528.broadway.core.StoryConfig
import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.io.flow.Flow
import com.github.ldaniels528.broadway.core.io.trigger.Trigger.IOStats
import com.github.ldaniels528.tabular.Tabular
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Represents an execution trigger
  * @author lawrence.daniels@gmail.com
  */
trait Trigger {
  protected val logger = LoggerFactory.getLogger(getClass)
  private val tabular = new Tabular()

  def id: String

  def execute(story: StoryConfig)(implicit ec: ExecutionContext): Unit

  def process(processFlows: Seq[(Flow, Scope)])(implicit ec: ExecutionContext) = {
    Future.sequence {
      processFlows map { case (flow, scope) =>
        logger.info(s"Starting to process flow '${flow.id}'...")
        implicit val myScope = scope
        val task = flow.execute(scope)

        task onComplete {
          case Success(_) =>
            val stats = generateStatistics(Seq((flow, scope)))
            tabular.synchronized {
              tabular.transform(stats) foreach logger.info
            }
          case Failure(e) =>
            logger.error(s"Processing failed: ${e.getMessage}", e)
        }
        task
      }
    }
  }

  protected def createScope(story: StoryConfig, flow: Flow) = {
    val scope = Scope()
    scope ++= story.properties
    scope ++= Seq(
      "date()" -> (() => new Date()),
      "trigger.id" -> id,
      "trigger.type" -> getClass.getSimpleName
    )
    scope
  }

  protected def generateStatistics(processFlows: Seq[(Flow, Scope)]) = {
    processFlows.flatMap { case (flow, scope) =>
      flow.devices map { d =>
        val action = if (d.isInstanceOf[OutputSource]) "writes" else "reads"
        IOStats(
          flow = flow.id,
          source = d.id,
          action = action,
          count = d.getStatistics(scope).count,
          processTimeMsec = d.getStatistics(scope).elapsedTimeMillis,
          avgRecordsPerSec = (d.getStatistics(scope).avgRecordsPerSecond * 10).toInt / 10d)
      } sortBy (_.flow)
    }
  }

}

/**
  * Trigger Companion Object
  * @author lawrence.daniels@gmail.com
  */
object Trigger {

  /**
    * I/O Statistics
    */
  case class IOStats(flow: String, source: String, action: String, count: Long, processTimeMsec: Long, avgRecordsPerSec: Double)

}