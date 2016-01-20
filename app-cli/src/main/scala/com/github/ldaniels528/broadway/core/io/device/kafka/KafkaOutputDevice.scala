package com.github.ldaniels528.broadway.core.io.device.kafka

import java.util.UUID

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.github.ldaniels528.broadway.cli.actors.TaskActorSystem
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.kafka.KafkaOutputDevice.{Die, asyncActor}
import com.github.ldaniels528.broadway.core.io.device.{OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.OutputLayout
import com.github.ldaniels528.broadway.core.RuntimeContext
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Kafka Output Device
  *
  * @author lawrence.daniels@gmail.com
  */
case class KafkaOutputDevice(id: String, topic: String, zk: ZkProxy, layout: OutputLayout) extends OutputDevice with StatisticsGeneration {
  private val logger = LoggerFactory.getLogger(getClass)
  private val publisher = KafkaPublisher(zk)

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
    implicit val timeout: Timeout = 1.hour
    (asyncActor ? Die) map { _ =>
      logger.info("Closing Kafka publisher...")
      publisher.close()
    }
  }

  override def open(rt: RuntimeContext): Unit = publisher.open()

  override def write(data: Data): Int = {
    val key = ByteBufferUtils.uuidToBytes(UUID.randomUUID())
    val message = layout.encode(count, data).map(_.getBytes).getOrElse(Array.empty)
    implicit val timeout: Timeout = 15.seconds
    (asyncActor ? publisher.publish(topic, key, message)) foreach { _ =>
      updateCount(1)
    }
    updateCount(0)
  }

}

/**
  * Kafka Output Device Companion Object
  */
object KafkaOutputDevice {
  val asyncActor = TaskActorSystem.system.actorOf(Props[AsyncResultActor])
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