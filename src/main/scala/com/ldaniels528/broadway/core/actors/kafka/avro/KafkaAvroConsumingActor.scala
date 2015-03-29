package com.ldaniels528.broadway.core.actors.kafka.avro

import akka.actor.{Actor, ActorRef}
import com.ldaniels528.broadway.core.actors.kafka.avro.KafkaAvroConsumingActor._
import com.ldaniels528.trifecta.io.kafka.KafkaMicroConsumer
import org.apache.avro.Schema
import org.slf4j.LoggerFactory

/**
 * Kafka-Avro Consuming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaAvroConsumingActor(topic: String, schemaString: String) extends Actor {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  @transient private lazy val schema = new Schema.Parser().parse(schemaString)
  @transient private var consumer_? : Option[KafkaMicroConsumer] = None

  override def preStart() = {
    consumer_? // TODO finish this at some point
  }

  override def postStop() = {
    consumer_?.foreach(_.close())
    consumer_? = None
  }

  override def receive = {
    case Consume(target) =>
      target ! "X" // TODO finish this at some point

    case message =>
      logger.warn(s"Unhandled message $message")
      unhandled(message)
  }

  private def consumer: KafkaMicroConsumer = {
    null // TODO finish this at some point
  }

}

/**
 * Kafka-Avro Consuming Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaAvroConsumingActor {

  case class Consume(target: ActorRef)

}
