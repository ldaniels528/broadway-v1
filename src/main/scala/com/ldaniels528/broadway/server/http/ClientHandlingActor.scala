package com.ldaniels528.broadway.server.http

import java.io.ByteArrayOutputStream
import java.net.URL

import akka.actor.{Actor, ActorLogging, Props, _}
import akka.util.Timeout
import com.ldaniels528.broadway.util.JsonHelper
import com.ldaniels528.commons.helpers.ResourceHelper._
import org.apache.commons.io.IOUtils
import spray.can.Http
import spray.can.Http.RegisterChunkHandler
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Client Handling Actor
 * @author lawrence.daniels@gmail.com
 */
class ClientHandlingActor(bsc: ServerContext) extends Actor with ActorLogging {
  implicit val timeout: Timeout = 1.second // for the actor 'asks'

  override def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! getHttpContent("/index.htm")

    case HttpRequest(GET, Uri.Path("/api/narratives"), _, _, _) =>
      sender ! HttpResponse(entity = HttpEntity(
        contentType = ContentType(mediaType = `application/json`), string = JsonHelper.toJsonString(bsc.narratives)))

    case req@HttpRequest(POST, Uri.Path("/api/file-upload"), headers, entity: HttpEntity.NonEmpty, protocol) =>
      handleFileUpload(req)

    case req@ChunkedRequestStart(HttpRequest(POST, Uri.Path("/api/file-upload"), _, _, _)) =>
      handleChunkedRequestStart(req)

    case req: HttpRequest => sender ! getHttpContent(req)

    // see https://github.com/spray/spray/tree/master/examples/spray-can/simple-http-server/src/main/scala/spray/examples
    case Timedout(HttpRequest(method, uri, _, _, _)) =>
      sender ! HttpResponse(status = 500, entity = s"The $method request to '$uri' has timed out...")
  }

  private def getHttpContent(req: HttpRequest): HttpResponse = getHttpContent(req.uri.path.toString())

  private def getHttpContent(resource: String): HttpResponse = {
    val resourcePath = if (resource.startsWith("/app")) resource else s"/app$resource"
    val mimeType = guessMIMEType(resource)
    log.info(s"Resource requested: $resource ~> $resourcePath [$mimeType]")

    getResource(resourcePath) match {
      case Some(url) =>
        url.openStream()
        HttpResponse(entity = HttpEntity(mimeType, readContent(url)))
      case None =>
        log.info(s"Resource [$resourcePath] not found")
        HttpResponse(status = 404, entity = "Resource not found")
    }
  }

  private def guessMIMEType(resource: String) = {
    resource.toLowerCase match {
      case s if s.matches("\\S+[.]css") => `text/css`
      case s if s.matches("\\S+[.]htm") || s.matches("\\S+[.]html") => `text/html`
      case s if s.matches("\\S+[.]jp*g") => `image/jpeg`
      case s if s.matches("\\S+[.]js") => `application/javascript`
      case s if s.matches("\\S+[.]json") => `application/json`
      case s if s.matches("\\S+[.]gif") => `image/gif`
      case s if s.matches("\\S+[.]png") => `image/png`
      case s => `text/html`
    }
  }

  private def readContent(url: URL) = {
    new ByteArrayOutputStream(8192) use { out =>
      url.openStream() use { in =>
        IOUtils.copy(in, out)
      }
      out.toByteArray
    }
  }

  private def getResource(name: String) = Option(getClass.getResource(name))

  private def handleFileUpload(request: HttpRequest): Unit = {
    val parts = request.asPartStream()
    val client = sender()
    val handler = context.actorOf(Props(new FileUploadHandler(client, parts.head.asInstanceOf[ChunkedRequestStart])))
    parts.tail.foreach(handler ! _)
  }

  private def handleChunkedRequestStart(s: ChunkedRequestStart): Unit = {
    val client = sender()
    val handler = context.actorOf(Props(new FileUploadHandler(client, s)))
    sender ! RegisterChunkHandler(handler)
  }

}
