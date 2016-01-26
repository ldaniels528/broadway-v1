package com.github.ldaniels528.broadway.core.opcode.trigger

import com.github.ldaniels528.broadway.core.ETLConfig
import com.github.ldaniels528.broadway.core.io.IOStats
import com.github.ldaniels528.broadway.core.io.device.{OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.opcode.flow.Flow
import com.github.ldaniels528.broadway.core.scope.{GlobalScope, Scope}
import com.github.ldaniels528.tabular.Tabular
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Represents an execution trigger
  */
trait Trigger {
  protected val logger = LoggerFactory.getLogger(getClass)
  private val tabular = new Tabular()

  def id: String

  def execute(config: ETLConfig)(implicit ec: ExecutionContext): Unit

  def process(flows: Seq[Flow], args: Seq[(String, Any)] = Nil)(implicit ec: ExecutionContext) = {
    val tasks = Future.sequence(flows map { flow =>
      logger.info(s"Starting to process flow '${flow.id}'...")

      // create the root scope
      val scope = GlobalScope(flow)

      // populate the scope with the arguments
      args foreach { case (name, value) =>
        scope.add(name, value)
      }

      for {
        _ <- openDevices(scope, flow)
        _ <- Future.successful(flow.execute(scope))
      } yield (scope, flow)
    })

    // wait for all tasks to complete
    val closingTasks = for {
      results <- tasks
      _ = closeDevices(results)
    } yield results

    closingTasks onComplete {
      case Success(results) =>
        val stats = generateStatistics(results.map(_._2))
        tabular.synchronized {
          tabular.transform(stats) foreach logger.info
        }
      case Failure(e) =>
        logger.error("Processing failed", e)
    }
    Await.result(tasks, 14.days)
  }

  private def closeDevices(scopesAndFlows: Seq[(Scope, Flow)])(implicit ec: ExecutionContext) = Future.sequence {
    scopesAndFlows flatMap { case (scope, flow) =>
      flow.devices.map(_.close(scope))
    }
  }

  private def generateStatistics(flows: Seq[Flow]) = {
    flows.flatMap { flow =>
      flow.devices map { d =>
        val deviceType = if (d.isInstanceOf[OutputDevice]) "written" else "read"
        IOStats(flowId = flow.id, deviceId = d.id, deviceType = deviceType, count = d.count, avgRecordsPerSecond = d match {
          case s: StatisticsGeneration => (s.avgRecordsPerSecond * 10).toInt / 10d
          case _ => 0d
        })
      } sortBy (_.flowId)
    }
  }

  private def openDevices(scope: Scope, flow: Flow) = Future.successful {
    logger.info("Opening the input and output devices...")
    flow.devices foreach (_.open(scope))
  }

}