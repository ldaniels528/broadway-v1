package com.ldaniels528.broadway.core.actors.kafka

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.ldaniels528.broadway.core.actors.kafka.KafkaAvroConsumingActor.startConsumer
import com.ldaniels528.broadway.core.actors.kafka.KafkaConsumingActor.{StartConsuming, StopConsuming}
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.ldaniels528.trifecta.io.kafka.{Broker, KafkaMicroConsumer}
import com.ldaniels528.trifecta.io.zookeeper.ZKProxy
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

/**
 * Kafka-Avro Consuming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaAvroConsumingActor(zkConnectionString: String, schemaString: String) extends Actor with ActorLogging {
  private val registrations = TrieMap[(String, ActorRef), Future[Seq[Unit]]]()

  import context.dispatcher

  override def receive = {
    case StartConsuming(topic, target) =>
      log.info(s"Registering topic '$topic' to $target...")
      registrations.putIfAbsent((topic, target), startConsumer(zkConnectionString, topic, schema, target))

    case StopConsuming(topic, target) =>
      log.info(s"Canceling registration of topic '$topic' to $target...")
      registrations.get((topic, target)) match {
        case Some(task) => // TODO cancel the future
        case None =>
      }

    case message =>
      log.error(s"Unhandled message $message")
      unhandled(message)
  }

  private def schema = new Schema.Parser().parse(schemaString)

}

/**
 * Kafka-Avro Consuming Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaAvroConsumingActor {

  private def startConsumer(zkConnectionString: String,
                            topic: String,
                            schema: Schema,
                            target: ActorRef)(implicit ec: ExecutionContext): Future[Seq[Unit]] = {
    implicit val zk = ZKProxy(zkConnectionString)
    val brokerList = KafkaMicroConsumer.getBrokerList
    val brokers = (0 to brokerList.size - 1) zip brokerList map { case (n, b) => Broker(b.host, b.port, n) }
    KafkaMicroConsumer.observe(topic, brokers) { md =>
      target ! AvroMessageReceived(topic, md.partition, md.offset, md.key, AvroConversion.decodeRecord(schema, md.message))
    }
  }

  /**
   * This message is sent to all registered actors when a message is available for
   * the respective topic.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.csv")
   * @param partition the topic partition from which the message was received.
   * @param offset the partition offset from which the message was received.
   * @param key the message key
   * @param message the Avro message object
   */
  case class AvroMessageReceived(topic: String, partition: Int, offset: Long, key: Array[Byte], message: GenericRecord)

}
