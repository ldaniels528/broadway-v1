package com.github.ldaniels528.broadway.core.io.trigger

import com.github.ldaniels528.broadway.core.StoryConfig
import com.github.ldaniels528.broadway.core.io.device.{AsynchronousOutputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.flow.Flow
import com.github.ldaniels528.broadway.core.io.trigger.Trigger.IOStats
import com.github.ldaniels528.broadway.core.scope.{InheritedScope, Scope}
import com.github.ldaniels528.tabular.Tabular
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Represents an execution trigger
  */
trait Trigger {
  protected val logger = LoggerFactory.getLogger(getClass)
  private val tabular = new Tabular()

  def id: String

  def execute(config: StoryConfig)(implicit ec: ExecutionContext): Unit

  def process(processFlows: Seq[(Flow, Scope)])(implicit ec: ExecutionContext) = {
    Future.sequence {
      processFlows map { case (flow, scope) =>
        logger.info(s"Starting to process flow '${flow.id}'...")
        openDevices(scope, flow)
        flow.execute(scope)

        val task = Future.sequence {
          flow.devices flatMap {
            case src: AsynchronousOutputSource => Some(src)
            case _ => None
          } map (_.allWritesCompleted(scope))
        }

        task onComplete {
          case Success(_) =>
            closeDevices(scope, flow)
            val stats = generateStatistics(Seq((flow, scope)))
            tabular.synchronized {
              tabular.transform(stats) foreach logger.info
            }
          case Failure(e) =>
            logger.error(s"Processing failed: ${e.getMessage}", e)
        }
        task
      }
    } map(_.flatten)
  }

  protected def createScope(rootScope: Scope, flow: Flow) = {
    val scope = InheritedScope(rootScope)
    scope ++= Seq(
      "trigger.id" -> id,
      "trigger.type" -> getClass.getSimpleName,

      "flow.input.id" -> flow.input.id,
      "flow.input.count" -> (() => flow.input.getStatistics(scope).count),
      "flow.input.offset" -> (() => flow.input.getStatistics(scope).offset),

      "flow.output.id" -> flow.output.id,
      "flow.output.count" -> (() => flow.output.getStatistics(scope).count),
      "flow.output.offset" -> (() => flow.output.getStatistics(scope).offset)
    )
    scope
  }

  protected def generateStatistics(processFlows: Seq[(Flow, Scope)]) = {
    processFlows.flatMap { case (flow, scope) =>
      flow.devices map { d =>
        val action = if (d.isInstanceOf[OutputSource]) "writes" else "reads"
        IOStats(
          flow = flow.id,
          device= d.id,
          action = action,
          count = d.getStatistics(scope).count,
          processTimeMsec = d.getStatistics(scope).elapsedTimeMillis,
          avgRecordsPerSec = (d.getStatistics(scope).avgRecordsPerSecond * 10).toInt / 10d)
      } sortBy (_.flow)
    }
  }

  protected def closeDevices(scope: Scope, flow: Flow) = Future.successful {
    logger.info("Closing the input and output devices...")
    flow.devices foreach (_.close(scope))
  }

  protected def openDevices(scope: Scope, flow: Flow) = Future.successful {
    logger.info("Opening the input and output devices...")
    flow.devices foreach (_.open(scope))
  }

}

/**
  * Trigger Companion Object
  */
object Trigger {

  /**
    * I/O Statistics
    */
  case class IOStats(flow: String, device: String, action: String, count: Long, processTimeMsec: Long, avgRecordsPerSec: Double)

}