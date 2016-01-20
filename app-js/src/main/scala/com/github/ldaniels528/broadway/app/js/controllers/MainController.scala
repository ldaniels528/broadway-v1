package com.github.ldaniels528.broadway.app.js.controllers

import com.github.ldaniels528.broadway.app.js.models.MainTab
import com.github.ldaniels528.scalascript.{Controller, Scope}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Main Controller
  */
class MainController($scope: MainScope) extends Controller {
  $scope.tabs = js.Array(
    MainTab(
      name = "Configuration",
      contentURL = "/processing",
      icon = "fa fa-bolt",
      active = true
    ),
    MainTab(
      name = "Processing",
      contentURL = "/processing",
      icon = "fa fa-bolt",
      active = false
    ),
    MainTab(
      name = "Archival",
      contentURL = "/processing",
      icon = "fa fa-bolt",
      active = false
    ))

  $scope.tab = $scope.tabs.headOption.orUndefined

  $scope.isActiveTab = (aTab: js.UndefOr[MainTab]) => $scope.tab == aTab

}

/**
  * Main Scope
  */
@js.native
trait MainScope extends Scope {
  // properties
  var tab: js.UndefOr[MainTab] = js.native
  var tabs: js.Array[MainTab] = js.native

  // functions
  var isActiveTab: js.Function1[js.UndefOr[MainTab], Boolean] = js.native

}