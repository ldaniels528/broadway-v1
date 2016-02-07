package com.github.ldaniels528.broadway.web.routes

import akka.actor.ActorLogging
import com.github.ldaniels528.broadway.web.views.IndexView
import spray.http.MediaTypes._
import spray.routing.HttpService

/**
  * Application Routes
  * @author lawrence.daniels@gmail.com
  */
trait ApplicationRoutes extends HttpService with IndexView {
  self: ActorLogging =>

  val applicationRoutes =
    get {
      pathSingleSlash {
        respondWithMediaType(`text/html`) {
          complete {
            indexView
          }
        }
      } ~
        pathPrefix("assets") {
          getFromResourceDirectory("assets")
        } ~
        pathPrefix("webjars") {
          get {
            getFromResourceDirectory("META-INF/resources/webjars")
          }
        }
    }

}
