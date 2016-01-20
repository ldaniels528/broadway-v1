package com.github.ldaniels528.broadway.app.js

import com.github.ldaniels528.broadway.app.js.controllers.MainController
import com.github.ldaniels528.broadway.app.js.services.ServerSideEventsService
import com.github.ldaniels528.scalascript._
import com.github.ldaniels528.scalascript.core.HttpProvider
import com.github.ldaniels528.scalascript.extensions.{RouteProvider, RouteTo}
import org.scalajs.dom.console

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/**
  * Broadway JavaScript Application
  */
object BroadwayJsApp extends js.JSApp {
  val appName = "broadway"

  @JSExport
  override def main(): Unit = {
    // create the application
    val module = angular.createModule(appName, js.Array(
      "ngAnimate", "ngCookies", "ngResource", "ngRoute", "ngSanitize", "hljs", "toaster", "ui.bootstrap"
    ))

    // configure the controllers
    module.controllerOf[MainController]("MainController")

    // configure the services
    module.serviceOf[ServerSideEventsService]("ServerSideEventsService")

    // configure the application
    module.config({ ($httpProvider: HttpProvider, $routeProvider: RouteProvider) =>
      $routeProvider
        .when("/decoders", RouteTo(templateUrl = "/assets/views/decoders.html"))
        .when("/inspect", RouteTo(templateUrl = "/assets/views/inspect/index.html", reloadOnSearch = false))
        .when("/observe", RouteTo(templateUrl = "/assets/views/observe.html"))
        .when("/publish", RouteTo(templateUrl = "/assets/views/publish.html"))
        .when("/query", RouteTo(templateUrl = "/assets/views/query.html"))
        //.otherwise(RouteTo(redirectTo = "/"))
      ()
    })

    // start the application
    module.run({ ($rootScope: RootScope, ServerSideEventsService: ServerSideEventsService) =>
      $rootScope.version = "0.1.0"

      console.log("Initializing application...")
      ServerSideEventsService.connect()
      ()
    })

  }

}

/**
  * Broadway Application Root Scope
  *
  * @author lawrence.daniels@gmail.com
  */
@js.native
trait RootScope extends Scope {
  var version: js.UndefOr[String] = js.native
}