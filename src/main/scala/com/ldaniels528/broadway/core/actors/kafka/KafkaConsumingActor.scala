package com.ldaniels528.broadway.core.actors.kafka

import akka.actor.ActorRef
import com.ldaniels528.broadway.core.actors.BroadwayActor
import com.ldaniels528.broadway.core.actors.kafka.KafkaConsumingActor._
import com.ldaniels528.broadway.core.actors.kafka.KafkaHelper._
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.ldaniels528.trifecta.io.kafka.{KafkaMacroConsumer, KafkaMicroConsumer}
import com.ldaniels528.trifecta.io.zookeeper.ZKProxy
import com.ldaniels528.commons.helpers.ResourceHelper._
import kafka.common.TopicAndPartition
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

import scala.collection.concurrent.TrieMap

/**
 * Kafka Message Consuming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaConsumingActor(zkConnect: String) extends BroadwayActor {
  private implicit lazy val zk = getZKProxy(zkConnect)
  private val consumerGroups = TrieMap[(String, String, ActorRef), KafkaMacroConsumer]()

  import context.dispatcher

  override def receive = {
    case StartConsuming(topic, groupId, target, avroSchema) =>
      log.info(s"Registering topic '$topic' and group ID '$groupId' to $target...")
      consumerGroups.putIfAbsent((topic, groupId, target), startConsumer(topic, groupId, avroSchema, target))
      ()

    case StopConsuming(topic, groupId, target) =>
      log.info(s"Canceling registration of topic '$topic' and group ID '$groupId' to $target...")
      consumerGroups.remove((topic, groupId, target)) foreach (_.close())

    case message =>
      log.error(s"Unhandled message $message")
      unhandled(message)
  }

  private def startConsumer(topic: String, groupId: String, avroSchema: Option[Schema], target: ActorRef): KafkaMacroConsumer = {
    val partitions = KafkaMicroConsumer.getTopicPartitions(topic)
    log.info(s"Topic $topic has ${partitions.size} partitions...")

    /*
    // ensure the group ID exists for each partition
    partitions.foreach { partition =>
      new KafkaMicroConsumer(new TopicAndPartition(topic, partition), zk.getBrokerList) use { consumer =>
        val consumerOffset = consumer.fetchOffsets(groupId)
        consumerOffset.foreach(offset => log.info(s"$topic:$partition/$groupId offset is $offset"))

        if (consumerOffset.isEmpty) {
          consumer.getFirstOffset.foreach { offset =>
            log.info(s"Committing initial offset for $topic:$partition as $offset...")
            consumer.commitOffsets(groupId, offset, "Broadway setting initial offset")
          }
        }
      }
    }*/

    // start the consumer
    avroSchema match {
      case Some(schema) => startAvroConsumer(topic, groupId, parallelism = partitions.size, schema, target)
      case None => startBinaryConsumer(topic, groupId, parallelism = partitions.size, target)
    }
  }

  private def startAvroConsumer(topic: String, groupId: String, parallelism: Int, schema: Schema, target: ActorRef): KafkaMacroConsumer = {
    val consumer = KafkaMacroConsumer(zkConnect, groupId)
    consumer.observe(topic, parallelism) { md =>
      target ! AvroMessageReceived(topic, md.partition, md.offset, md.key, AvroConversion.decodeRecord(schema, md.message))
    }
    consumer
  }

  private def startBinaryConsumer(topic: String, groupId: String, parallelism: Int, target: ActorRef): KafkaMacroConsumer = {
    val consumer = KafkaMacroConsumer(zkConnect, groupId)
    consumer.observe(topic, parallelism) { md =>
      target ! BinaryMessageReceived(topic, md.partition, md.offset, md.key, md.message)
    }
    consumer
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
  case class StartConsuming(topic: String, groupId: String, target: ActorRef, avroSchema: Option[Schema] = None)

  /**
   * Cancels a registration; causing no future messages to be sent to the recipient.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.csv")
   * @param target the given recipient actor; the actor that is to receive the messages
   */
  case class StopConsuming(topic: String, groupId: String, target: ActorRef)

  /**
   * This message is sent to all registered actors when a message is available for
   * the respective topic.
   * @param topic the given Kafka topic (e.g. "quotes.yahoo.csv")
   * @param partition the topic partition from which the message was received.
   * @param offset the partition offset from which the message was received.
   * @param key the message key
   * @param message the message data
   */
  case class BinaryMessageReceived(topic: String, partition: Int, offset: Long, key: Array[Byte], message: Array[Byte])

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