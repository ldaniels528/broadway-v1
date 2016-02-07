package com.github.ldaniels528.broadway.web

import akka.actor.{ActorSystem, Props}
import com.github.ldaniels528.broadway.web.actors.ApiActor
import spray.servlet.WebBoot

/**
  * Broadway Web Application Boot
  * @author lawrence.daniels@gmail.com
  */
class BroadwayBoot extends WebBoot {
  //we need an ActorSystem to host our application in
  implicit val system = ActorSystem("BroadwayApiApp")

  //create apiActor
  val apiActor = system.actorOf(Props[ApiActor], "apiActor")

  // the service actor replies to incoming HttpRequests
  val serviceActor = apiActor

}