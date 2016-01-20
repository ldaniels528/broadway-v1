package com.github.ldaniels528.broadway.app.js.models

import com.github.ldaniels528.scalascript.util.ScalaJsHelper._

import scala.scalajs.js

/**
  * Main Tab
  */
@js.native
trait MainTab extends js.Object {
  var name: String = js.native
  var contentURL: String = js.native
  var imageURL: String = js.native
  var active: Boolean = js.native
}

/**
  * Main Tab Companion Object
  *
  * @author lawrence.daniels@gmail.com
  */
object MainTab {

  def apply(name: String, contentURL: String, icon: String, active: Boolean = false) = {
    val tab = makeNew[MainTab]
    tab.name = name
    tab.contentURL = contentURL
    tab.imageURL = icon
    tab.active = active
    tab
  }

}