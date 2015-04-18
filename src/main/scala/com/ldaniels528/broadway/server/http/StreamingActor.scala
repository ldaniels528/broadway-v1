package com.ldaniels528.broadway.server.http

import akka.actor.{Actor, ActorLogging, ActorRef, _}
import com.ldaniels528.broadway.server.http.StreamingActor._
import spray.can.Http
import spray.http.{ChunkedResponseStart, DateTime, HttpResponse, MessageChunk, _}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Streaming Actor
 * @param client the client actor
 * @param count the number of bytes to stream
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class StreamingActor(client: ActorRef, count: Int) extends Actor with ActorLogging {

  import context.dispatcher

  log.debug("Starting streaming response ...")

  // we use the successful sending of a chunk as trigger for scheduling the next chunk
  client ! ChunkedResponseStart(HttpResponse(entity = " " * 2048)).withAck(Ok(count))

  override def receive = {
    case Ok(0) =>
      log.info("Finalizing response stream ...")
      client ! MessageChunk("\nStopped...")
      client ! ChunkedMessageEnd
      context.stop(self)

    case Ok(remaining) =>
      log.info("Sending response chunk ...")
      context.system.scheduler.scheduleOnce(100 millis span) {
        client ! MessageChunk(DateTime.now.toIsoDateTimeString + ", ").withAck(Ok(remaining - 1))
      }
      ()

    case x: Http.ConnectionClosed =>
      log.info("Canceling response stream due to {} ...", x)
      context.stop(self)
  }
}

/**
 * Streaming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object StreamingActor {

  /**
   * simple case class whose instances we use as send confirmation message for streaming chunks
   */
  case class Ok(remaining: Int)

}