package com.ldaniels528.broadway.core.actors.kafka

import akka.actor.{Actor, ActorLogging}
import com.datastax.driver.core.utils.UUIDs
import com.ldaniels528.broadway.core.actors.kafka.KafkaPublishingActor.{PublishAvro, Publish}
import com.ldaniels528.trifecta.io.ByteBufferUtils
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.ldaniels528.trifecta.io.kafka.{Broker, KafkaPublisher}
import org.apache.avro.generic.GenericRecord

import scala.util.{Failure, Success}

/**
 * Kafka-Avro Publishing Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaPublishingActor(topic: String, brokers: String) extends Actor with ActorLogging {
  private val publisher = KafkaPublisher(Broker.parseBrokerList(brokers))

  override def preStart() = publisher.open()

  override def postStop() = publisher.close()

  override def receive = {
    case Publish(message, key, attempts) => publish(topic, key, message, attempts)
    case PublishAvro(record, key, attempts) => publish(topic, key, AvroConversion.encodeRecord(record), attempts)
    case message =>
      log.error(s"Unhandled message $message")
      unhandled(message)
  }

  private def publish(topic: String, key: Array[Byte], message: Array[Byte], attempts: Int = 1) {
    publisher.publish(topic, key, message) match {
      case Success(_) =>
      case Failure(e: Exception) =>
        if (!retryPublish(key, message, attempts)) {
          log.error(s"Failed ($attempts times) to get a connection to publish message", e)
        }
      case Failure(e) =>
        log.error(s"Failed to publish message: ${e.getMessage}", e)
    }
  }

  private def retryPublish(key: Array[Byte], message: Array[Byte], attempts: Int): Boolean = {
    val retry = attempts < 3
    if (retry) {
      self ! Publish(key, message, attempts + 1)
    }
    retry
  }

}

/**
 * Kafka-Avro Publishing Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaPublishingActor {

  private def makeUUID = ByteBufferUtils.uuidToBytes(UUIDs.timeBased())

  case class Publish(message: Array[Byte], key: Array[Byte] = makeUUID, attempt: Int = 0)

  case class PublishAvro(record: GenericRecord, key: Array[Byte] = makeUUID, attempt: Int = 0)

}