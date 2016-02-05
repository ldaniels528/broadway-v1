package com.github.ldaniels528.broadway.core.io.device.impl

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.ldaniels528.broadway.core.actors.BroadwayActorSystem
import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.impl.KafkaOutputSource._
import com.github.ldaniels528.broadway.core.io.device.{AsynchronousOutputSupport, OutputSource}
import com.github.ldaniels528.broadway.core.support.kafka.{ByteBufferUtils, KafkaPublisher, ZkProxy}
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.record.Record
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Kafka Output Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class KafkaOutputSource(id: String, topic: String, zk: ZkProxy, layout: Layout) extends OutputSource with AsynchronousOutputSupport {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val publisher = KafkaPublisher(zk)

  override def allWritesCompleted(implicit scope: Scope, ec: ExecutionContext) = {
    implicit val timeout: Timeout = 1.hour
    (asyncActor ? Die) map { _ =>
      logger.info("Closing Kafka publisher...")
      this
    }
  }

  override def close(implicit scope: Scope) = publisher.close()

  override def open(implicit scope: Scope) = {
    scope ++= Seq(
      "flow.output.topic" -> topic,
      "flow.output.connection" -> zk.connectionString,
      "flow.output.count" -> (() => getStatistics.count),
      "flow.output.offset" -> (() => getStatistics.offset)
    )
    publisher.open()
  }

  override def writeRecord(record: Record)(implicit scope: Scope) = {
    implicit val timeout: Timeout = 15.seconds
    val key = ByteBufferUtils.uuidToBytes(UUID.randomUUID())
    val message = record.convertToBinary
    (asyncActor ? publisher.publish(topic, key, message)) foreach (_ => updateCount(1))
    0
  }

}

/**
  * Kafka Output IOSource Companion Object
  */
object KafkaOutputSource {
  val asyncActor = BroadwayActorSystem.system.actorOf(Props[AsyncResultActor])
  var dying = false

  /**
    * Asynchronous Results Actor
    */
  class AsyncResultActor() extends Actor with ActorLogging {
    private val tasks = TrieMap[java.util.concurrent.Future[_], ActorRef]()

    val processingPromise =
      context.system.scheduler.schedule(0.seconds, 100.millis, self, Check)

    override def receive = {
      case task: java.util.concurrent.Future[_] =>
        tasks.put(task, sender())
        ()

      case Check =>
        tasks.foreach { case (task, sender) =>
          if (task.isDone) {
            if (dying && tasks.size % 2500 == 0) {
              log.info(s"Draining queue: ${tasks.size} messages")
            }
            sender ! task.get()
            tasks.remove(task)
          }
        }
        ()

      case Die =>
        dying = true
        log.info(s"Queue contains ${tasks.size} messages")
        val mySender = sender()
        if (tasks.isEmpty) {
          mySender ! Dead
          stopThyself()
        }
        else {
          context.system.scheduler.scheduleOnce(1.second, mySender, Die)
        }
        ()

      case message =>
        log.warning(s"Unhandled message '$message'")
        unhandled(message)
    }

    private def stopThyself() = {
      processingPromise.cancel()
      context.stop(self)
    }
  }

  case object Check

  case object Die

  case object Dead

}