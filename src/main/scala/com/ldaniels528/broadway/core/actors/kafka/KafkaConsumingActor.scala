package com.ldaniels528.broadway.core.actors.kafka

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.ldaniels528.broadway.core.actors.kafka.KafkaConsumingActor.{StartConsuming, StopConsuming, startConsumer}
import com.ldaniels528.trifecta.io.kafka.{Broker, KafkaMicroConsumer}
import com.ldaniels528.trifecta.io.zookeeper.ZKProxy

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

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
      registrations.putIfAbsent((topic, target), startConsumer(zkConnectionString, topic, target))

    case StopConsuming(topic, target) =>
      log.info(s"Canceling registration of topic '$topic' to $target...")
      registrations.get((topic, target)) match {
        case Some(task) => // TODO cancel the future
        case None =>
      }

    case message =>
      unhandled(message)
  }
}

/**
 * Kafka Message Consuming Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaConsumingActor {

  private def startConsumer(zkConnectionString: String,
                                   topic: String,
                                   target: ActorRef)(implicit ec: ExecutionContext): Future[Seq[Unit]] = {
    implicit val zk = ZKProxy(zkConnectionString)
    val brokerList = KafkaMicroConsumer.getBrokerList
    val brokers = (0 to brokerList.size - 1) zip brokerList map { case (n, b) => Broker(b.host, b.port, n) }
    KafkaMicroConsumer.observe(topic, brokers) { md =>
      target ! MessageReceived(topic, md.partition, md.offset, md.key, md.message)
    }
  }

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