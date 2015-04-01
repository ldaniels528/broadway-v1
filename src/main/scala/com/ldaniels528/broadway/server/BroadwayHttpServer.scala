package com.ldaniels528.broadway.server

import java.io.{File, FileInputStream, FileOutputStream, InputStream}

import _root_.spray.can.Http
import _root_.spray.can.Http.RegisterChunkHandler
import _root_.spray.http.HttpHeaders.{RawHeader, `Content-Disposition`}
import _root_.spray.http.HttpMethods._
import _root_.spray.http.MediaTypes._
import _root_.spray.http.parser.HttpParser
import _root_.spray.http.{ChunkedMessageEnd, ChunkedRequestStart, ChunkedResponseStart, ContentType, DateTime, HttpEntity, HttpHeaders, HttpRequest, HttpResponse, MessageChunk, MultipartMediaType, SetRequestTimeout, Timedout, Uri}
import _root_.spray.io.CommandWrapper
import akka.actor.{Actor, ActorLogging, ActorRef, Props, _}
import akka.io.IO
import akka.io.Tcp.Bound
import akka.pattern.ask
import akka.util.Timeout
import com.ldaniels528.broadway.server.BroadwayHttpServer._
import org.jvnet.mimepull.{MIMEMessage, MIMEPart}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps

/**
 * Broadway Http Server
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayHttpServer(host: String, port: Int)(implicit system: ActorSystem) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val listenerActor = system.actorOf(Props[ClientHandlingActor], "clientHandler")
  import system.dispatcher

  def start(): Unit = {
    // start the HTTP server
    implicit val timeout: Timeout = 5 seconds
    val response = IO(Http) ? Http.Bind(listenerActor, interface = host, port = port)
    response.foreach {
      case Bound(interface) =>
        logger.info(s"Server is now bound to $interface")
      case outcome =>
        logger.error(s"Unexpected response $outcome")
    }
  }

}

/**
 * Broadway Http Server Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object BroadwayHttpServer {

  lazy val index = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h1>Say hello to
            <i>spray-can</i>
            !</h1>
          <p>Defined resources:</p>
          <ul>
            <li>
              <a href="/ping">/ping</a>
            </li>
            <li>
              <a href="/stream">/stream</a>
            </li>
            <li>
              <a href="/server-stats">/server-stats</a>
            </li>
            <li>
              <a href="/crash">/crash</a>
            </li>
            <li>
              <a href="/timeout">/timeout</a>
            </li>
            <li>
              <a href="/timeout/timeout">/timeout/timeout</a>
            </li>
            <li>
              <a href="/shutdown">/shutdown</a>
            </li>
          </ul>
          <p>Test file upload</p>
          <form action="/file-upload" enctype="multipart/form-data" method="post">
            <input type="file" name="datafile" multiple=" "></input>
            <br/>
            <input type="submit">Submit</input>
          </form>
        </body>
      </html>.toString()
    )
  )

  /**
   * Client Handling Actor
   * @author lawrence.daniels@gmail.com
   */
  class ClientHandlingActor() extends Actor with ActorLogging {
    implicit val timeout: Timeout = 1 second // for the actor 'asks'

    override def receive = {
      // when a new connection comes in we register ourselves as the connection handler
      case _: Http.Connected => sender ! Http.Register(self)

      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
        sender ! index

      case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
        sender ! HttpResponse(entity = "PONG!")

      case r@HttpRequest(POST, Uri.Path("/file-upload"), headers, entity: HttpEntity.NonEmpty, protocol) =>
        // emulate chunked behavior for POST requests to this path
        val parts = r.asPartStream()
        val client = sender()
        val handler = context.actorOf(Props(new FileUploadHandler(client, parts.head.asInstanceOf[ChunkedRequestStart])))
        parts.tail.foreach(handler !)

      case s@ChunkedRequestStart(HttpRequest(POST, Uri.Path("/file-upload"), _, _, _)) =>
        val client = sender()
        val handler = context.actorOf(Props(new FileUploadHandler(client, s)))
        sender ! RegisterChunkHandler(handler)

      case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")

      // see https://github.com/spray/spray/tree/master/examples/spray-can/simple-http-server/src/main/scala/spray/examples
      case Timedout(HttpRequest(method, uri, _, _, _)) =>
        sender ! HttpResponse(status = 500, entity = s"The $method request to '$uri' has timed out...")
    }
  }

  class StreamingActor(client: ActorRef, count: Int) extends Actor with ActorLogging {
    import context.dispatcher
    log.debug("Starting streaming response ...")

    // we use the successful sending of a chunk as trigger for scheduling the next chunk
    client ! ChunkedResponseStart(HttpResponse(entity = " " * 2048)).withAck(Ok(count))

    def receive = {
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

      case x: Http.ConnectionClosed =>
        log.info("Canceling response stream due to {} ...", x)
        context.stop(self)
    }
  }

  // simple case class whose instances we use as send confirmation message for streaming chunks
  case class Ok(remaining: Int)

  class FileUploadHandler(client: ActorRef, start: ChunkedRequestStart) extends Actor with ActorLogging {

    import start.request._

    client ! CommandWrapper(SetRequestTimeout(Duration.Inf))
    // cancel timeout

    val tmpFile = File.createTempFile("chunked-receiver", ".tmp", new File("/tmp"))
    tmpFile.deleteOnExit()
    val output = new FileOutputStream(tmpFile)
    val Some(HttpHeaders.`Content-Type`(ContentType(multipart: MultipartMediaType, _))) = header[HttpHeaders.`Content-Type`]
    val boundary = multipart.parameters("boundary")

    log.info(s"Got start of chunked request $method $uri with multipart boundary '$boundary' writing to $tmpFile")
    var bytesWritten = 0L

    def receive = {
      case c: MessageChunk =>
        log.debug(s"Got ${c.data.length} bytes of chunked request $method $uri")

        output.write(c.data.toByteArray)
        bytesWritten += c.data.length

      case e: ChunkedMessageEnd =>
        log.info(s"Got end of chunked request $method $uri")
        output.close()

        client ! HttpResponse(status = 200, entity = renderResult())
        client ! CommandWrapper(SetRequestTimeout(2.seconds)) // reset timeout to original value
        tmpFile.delete()
        context.stop(self)
    }

    import collection.JavaConverters._

    def renderResult(): HttpEntity = {
      val message = new MIMEMessage(new FileInputStream(tmpFile), boundary)
      // caution: the next line will read the complete file regardless of its size
      // In the end the mime pull parser is not a decent way of parsing multipart attachments
      // properly
      val parts = message.getAttachments.asScala.toSeq

      HttpEntity(`text/html`,
        <html>
          <body>
            <p>Got
              {bytesWritten}
              bytes</p>
            <h3>Parts</h3>{parts.map { part =>
            val name = fileNameForPart(part).getOrElse("<unknown>")
            <div>
              {name}
              :
              {part.getContentType}
              of size
              {sizeOf(part.readOnce())}
            </div>
          }}
          </body>
        </html>.toString()
      )
    }

    def fileNameForPart(part: MIMEPart): Option[String] =
      for {
        dispHeader <- part.getHeader("Content-Disposition").asScala.toSeq.lift(0)
        Right(disp: `Content-Disposition`) = HttpParser.parseHeader(RawHeader("Content-Disposition", dispHeader))
        name <- disp.parameters.get("filename")
      } yield name

    def sizeOf(is: InputStream): Long = {
      val buffer = new Array[Byte](65000)

      @tailrec def inner(cur: Long): Long = {
        val read = is.read(buffer)
        if (read > 0) inner(cur + read)
        else cur
      }

      inner(0)
    }
  }

}