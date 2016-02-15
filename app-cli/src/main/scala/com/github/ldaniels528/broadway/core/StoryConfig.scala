package com.github.ldaniels528.broadway.core

import java.io.FileInputStream

import com.github.ldaniels528.broadway.core.StoryConfig.StoryProperties
import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.archive.Archive
import com.github.ldaniels528.broadway.core.io.device.DataSource
import com.github.ldaniels528.broadway.core.io.filters.Filter
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.trigger.Trigger

import scala.collection.JavaConversions._

/**
  * Story Configuration
  * @author lawrence.daniels@gmail.com
  */
case class StoryConfig(id: String,
                       archives: Seq[Archive] = Nil,
                       devices: Seq[DataSource] = Nil,
                       filters: Seq[(String, Filter)] = Nil,
                       layouts: Seq[Layout] = Nil,
                       properties: Seq[StoryProperties] = Nil,
                       triggers: Seq[Trigger] = Nil)

/**
  * Story Configuration Companion Object
  */
object StoryConfig {

  /**
    * Story Configuration Properties
    */
  trait StoryProperties {

    def load(implicit scope: Scope): Seq[(String, String)]

  }

  /**
    * Story Configuration Properties File
    */
  case class StoryPropertiesFile(path: String) extends StoryProperties {

    override def load(implicit scope: Scope) = {
      val props = new java.util.Properties()
      props.load(new FileInputStream(scope.evaluateAsString(path)))
      props.toSeq
    }

  }

  /**
    * Story Configuration Properties Sequence
    */
  case class StoryPropertySeq(properties: Seq[(String, String)]) extends StoryProperties {

    override def load(implicit scope: Scope) = properties

  }

}