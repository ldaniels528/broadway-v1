package com.ldaniels528.broadway.core.actors.kafka

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.ldaniels528.broadway.core.actors.kafka.KafkaConsumingActor.{MessageReceived, StartConsuming, StopConsuming}
import com.ldaniels528.broadway.core.actors.kafka.KafkaHelper._
import com.ldaniels528.trifecta.io.kafka.KafkaMicroConsumer
import com.ldaniels528.trifecta.io.zookeeper.ZKProxy

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Kafka Message Consuming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaConsumingActor(zkConnectionString: String) extends Actor with ActorLogging {
  private val registrations = TrieMap[(String, ActorRef), Future[Seq[Unit]]]()

  import context.dispatcher

  override def receive = {
    case StartConsuming(topic, target) =>
      log.info(s"Registering topic '$topic' to $target...")
      registrations.putIfAbsent((topic, target), startConsumer(topic, target)) foreach {
        _ foreach { _ =>
          log.info(s"$topic: Watch has ended for $target")
          registrations.remove((topic, target))
        }
      }

    case StopConsuming(topic, target) =>
      log.info(s"Canceling registration of topic '$topic' to $target...")
      registrations.get((topic, target)) foreach { task =>
        // TODO cancel the future
      }

    case message =>
      log.error(s"Unhandled message $message")
      unhandled(message)
  }

  private def startConsumer(topic: String, target: ActorRef)(implicit ec: ExecutionContext): Future[Seq[Unit]] = {
    implicit val zk = ZKProxy(zkConnectionString)
    val task = KafkaMicroConsumer.observe(topic, zk.getBrokerList) { md =>
      target ! MessageReceived(topic, md.partition, md.offset, md.key, md.message)
    }
    task.foreach(_ => Try(zk.close()))
    task
  }
}

/**
 * Kafka Message Consuming Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaConsumingActor {

  /**
   * Registers the given recipient actor to receive messages from the given topic. The recipient
   * actor will receive { @link MessageReceived} messages when a message becomes available with the topic.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.csv")
   * @param target the given recipient actor; the actor that is to receive the messages
   */
  case class StartConsuming(topic: String, target: ActorRef)

  /**
   * Cancels a registration; causing no future messages to be sent to the recipient.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.csv")
   * @param target the given recipient actor; the actor that is to receive the messages
   */
  case class StopConsuming(topic: String, target: ActorRef)

  /**
   * This message is sent to all registered actors when a message is available for
   * the respective topic.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.csv")
   * @param partition the topic partition from which the message was received.
   * @param offset the partition offset from which the message was received.
   * @param key the message key
   * @param message the message data
   */
  case class MessageReceived(topic: String, partition: Int, offset: Long, key: Array[Byte], message: Array[Byte])

}