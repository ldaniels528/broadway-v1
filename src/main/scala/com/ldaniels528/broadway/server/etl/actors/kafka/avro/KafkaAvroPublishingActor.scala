package com.ldaniels528.broadway.server.etl.actors.kafka.avro

import java.util.UUID

import akka.actor.Actor
import com.ldaniels528.trifecta.io.ByteBufferUtils
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.ldaniels528.trifecta.io.kafka.{Broker, KafkaPublisher}
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

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
    case bytes: Array[Byte] =>
      publisher.publish(topic, key = makeUUID, message = bytes)
    case record: GenericRecord =>
      Try {
        publisher.publish(topic, key = makeUUID, message = AvroConversion.encodeRecord(record))
      } match {
        case Success(_) =>
        case Failure(e) =>
          logger.error(s"Failed to publish message: $record")
      }
    case message =>
      logger.warn(s"Unhandled message $message")
      unhandled(message)
  }

  def makeUUID = ByteBufferUtils.uuidToBytes(UUID.randomUUID)

}