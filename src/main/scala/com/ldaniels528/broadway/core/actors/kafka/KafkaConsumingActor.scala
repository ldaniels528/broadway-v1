package com.ldaniels528.broadway.core.actors.kafka

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.ldaniels528.broadway.core.actors.kafka.KafkaConsumingActor._
import com.ldaniels528.broadway.core.actors.kafka.KafkaHelper._
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.ldaniels528.trifecta.io.kafka.{Broker, KafkaMicroConsumer}
import com.ldaniels528.trifecta.io.zookeeper.ZKProxy
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

/**
 * Kafka Message Consuming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaConsumingActor(zkConnect: String) extends Actor with ActorLogging {
  private implicit lazy val zk = getZKProxy(zkConnect)
  private val registrations = TrieMap[(String, ActorRef), Future[Seq[Unit]]]()

  import context.dispatcher

  override def receive = {
    case StartConsuming(topic, target, avroSchema) =>
      log.info(s"Registering topic '$topic' to $target...")
      registrations.putIfAbsent((topic, target), startConsumer(topic, avroSchema, target)) foreach {
        _ foreach { _ =>
          log.info(s"$topic: My watch has ended [$target]")
          registrations.remove((topic, target))
        }
      }

    case StopConsuming(topic, target) =>
      log.info(s"Canceling registration of topic '$topic' to $target...")
      registrations.remove((topic, target)) foreach { task =>
        // TODO cancel the future -- use system.scheduler instead
      }

    case message =>
      log.error(s"Unhandled message $message")
      unhandled(message)
  }

  private def startConsumer(topic: String, avroSchema: Option[Schema], target: ActorRef): Future[Seq[Unit]] = {
    avroSchema match {
      case Some(schema) => startAvroConsumer(topic, schema, target)
      case None => startBinaryConsumer(topic, target)
    }
  }

  private def startBinaryConsumer(topic: String, target: ActorRef): Future[Seq[Unit]] = {
    KafkaMicroConsumer.observe(topic, zk.getBrokerList) { md =>
      target ! MessageReceived(topic, md.partition, md.offset, md.key, md.message)
    }
  }

  private def startAvroConsumer(topic: String, schema: Schema, target: ActorRef): Future[Seq[Unit]] = {
    implicit lazy val zk = ZKProxy(zkConnect)
    val brokerList = KafkaMicroConsumer.getBrokerList
    val brokers = (0 to brokerList.size - 1) zip brokerList map { case (n, b) => Broker(b.host, b.port, n) }
    KafkaMicroConsumer.observe(topic, brokers) { md =>
      target ! AvroMessageReceived(topic, md.partition, md.offset, md.key, AvroConversion.decodeRecord(schema, md.message))
    }
  }

}

/**
 * Kafka Message Consuming Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaConsumingActor {
  private[this] val zkProxies = TrieMap[String, ZKProxy]()

  /**
   * Ensures a single Zookeeper connection per server (connection string)
   * @param zkConnect the given Zookeeper connection string (e.g. "localhost:2181")
   * @return the Zookeeper proxy instance
   * @see http://stackoverflow.com/questions/19722354/curator-framework-objects-for-zookeeper
   */
  def getZKProxy(zkConnect: String) = zkProxies.getOrElseUpdate(zkConnect, ZKProxy(zkConnect))

  /**
   * Registers the given recipient actor to receive messages from the given topic. The recipient
   * actor will receive { @link MessageReceived} messages when a message becomes available with the topic.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.csv")
   * @param target the given recipient actor; the actor that is to receive the messages
   */
  case class StartConsuming(topic: String, target: ActorRef, avroSchema: Option[Schema] = None)

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

  /**
   * This message is sent to all registered actors when an Avro message is available for
   * the respective topic.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.avro")
   * @param partition the topic partition from which the message was received.
   * @param offset the partition offset from which the message was received.
   * @param key the message key
   * @param message the Avro message object
   */
  case class AvroMessageReceived(topic: String, partition: Int, offset: Long, key: Array[Byte], message: GenericRecord)

}