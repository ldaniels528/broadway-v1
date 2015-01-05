package com.ldaniels528.broadway.core.actors.kafka.avro

import java.util.UUID

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.kafka.avro.KafkaAvroPublishingActor._
import com.ldaniels528.trifecta.io.ByteBufferUtils
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.ldaniels528.trifecta.io.kafka.{Broker, KafkaPublisher}
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
 * Kafka-Avro Publishing Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaAvroPublishingActor(topic: String, brokers: String) extends Actor {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val publisher = KafkaPublisher(Broker.parseBrokerList(brokers))

  override def preStart() = publisher.open()

  override def postStop() = publisher.close()

  override def receive = {
    case Publish(message, key) => publish(topic, key, message)
    case PublishAvro(record, key) => publish(topic, key, AvroConversion.encodeRecord(record))
    case message =>
      logger.warn(s"Unhandled message $message")
      unhandled(message)
  }

  private def publish(topic: String, key: Array[Byte], message: Array[Byte], attempts: Int = 1) {
    publisher.publish(topic, key, message) match {
      case Success(_) =>
      case Failure(e: java.net.ConnectException) =>
        if (attempts < 3) {
          Thread.sleep(attempts * 5000)
          publish(topic, key, message, attempts + 1)
        }
        else logger.error(s"Failed ($attempts times) to get a connection to publish message")
      case Failure(e) =>
        logger.error(s"Failed to publish message: ${e.getMessage}")
    }
  }

}

/**
 * Kafka-Avro Publishing Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaAvroPublishingActor {

  private def makeUUID = ByteBufferUtils.uuidToBytes(UUID.randomUUID)

  case class Publish(message: Array[Byte], key: Array[Byte] = makeUUID)

  case class PublishAvro(record: GenericRecord, key: Array[Byte] = makeUUID)

}