package com.ldaniels528.broadway.core.actors.kafka

import com.datastax.driver.core.utils.UUIDs
import com.ldaniels528.broadway.core.actors.BroadwayActor
import com.ldaniels528.broadway.core.actors.kafka.KafkaPublishingActor.{Publish, PublishAvro}
import com.ldaniels528.trifecta.io.ByteBufferUtils
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.ldaniels528.trifecta.io.kafka.KafkaPublisher
import org.apache.avro.generic.GenericRecord

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success}

/**
 * Kafka-Avro Publishing Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaPublishingActor(zookeeperConnect: String) extends BroadwayActor {
  private lazy val publisher = KafkaPublishingActor.getPublisher(zookeeperConnect)

  override def receive = {
    case Publish(topic, message, key, attempts) => publish(topic, key, message, attempts)
    case PublishAvro(topic, record, key, attempts) => publish(topic, key, AvroConversion.encodeRecord(record), attempts)
    case message =>
      log.error(s"Unhandled message $message")
      unhandled(message)
  }

  /**
   * Attempts to publishing of the given binary message
   * @param topic the given topic (e.g. "quotes.yahoo.csv")
   * @param key the given key
   * @param message the given message
   * @param attempts the given attempt count for this message
   */
  private def publish(topic: String, key: Array[Byte], message: Array[Byte], attempts: Int = 1) {
    publisher.publish(topic, key, message) match {
      case Success(_) =>
      case Failure(e) =>
        if (!retryPublish(topic, key, message, attempts)) {
          log.error(s"Failed ($attempts times) to publish message: ${e.getMessage} (${e.getClass.getName}})")
        }
    }
  }

  /**
   * Re-attempts the publishing of the given binary message
   * @param topic the given topic (e.g. "quotes.yahoo.csv")
   * @param key the given key
   * @param message the given message
   * @param attempts the given attempt count for this message
   */
  private def retryPublish(topic: String, key: Array[Byte], message: Array[Byte], attempts: Int): Boolean = {
    val retry = attempts < 3
    if (retry) {
      self ! Publish(topic, message, key, attempts + 1)
    }
    retry
  }

}

/**
 * Kafka-Avro Publishing Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaPublishingActor {
  private[this] val publishers = TrieMap[String, KafkaPublisher]()

  /**
   * Retrieves a Kafka Publisher based on the given Zookeeper connection string
   * @param zookeeperConnect the given Zookeeper connection string
   * @return a [[KafkaPublisher Kafka Publisher]]
   */
  def getPublisher(zookeeperConnect: String): KafkaPublisher = {
    publishers.getOrElseUpdate(zookeeperConnect, {
      val publisher = KafkaPublisher(KafkaHelper.getBrokerList(zookeeperConnect))
      publisher.open()
      publisher
    })
  }

  /**
   * Generates a time-based UUID as a byte array
   * @return the given byte array
   */
  def makeUUID = ByteBufferUtils.uuidToBytes(UUIDs.timeBased())

  /**
   * Requests the publishing of the given binary message
   * @param topic the given topic (e.g. "quotes.yahoo.csv")
   * @param message the given message
   * @param key the given key
   * @param attempt the given attempt count for this message
   */
  case class Publish(topic: String, message: Array[Byte], key: Array[Byte] = makeUUID, attempt: Int = 0)

  /**
   * Requests the publishing of the given Avro message
   * @param topic the given topic (e.g. "quotes.yahoo.csv")
   * @param record the given message
   * @param key the given key
   * @param attempt the given attempt count for this message
   */
  case class PublishAvro(topic: String, record: GenericRecord, key: Array[Byte] = makeUUID, attempt: Int = 0)

}