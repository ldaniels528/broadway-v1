package com.github.ldaniels528.broadway.core.opcode.trigger

import java.io.File

import com.github.ldaniels528.broadway.core.opcode.flow.Flow

/**
  * Represents a File Feed
  */
case class FileFeed(name: String, matchType: String, flows: Seq[Flow]) {

  def matches(file: File): Boolean = {
    val fileName = file.getName
    matchType match {
      case "exact" => fileName.toLowerCase == name.toLowerCase
      case "regex" => fileName.toLowerCase.matches(name.toLowerCase)
      case "start" => fileName.toLowerCase.startsWith(name.toLowerCase)
      case "ends" => fileName.toLowerCase.endsWith(name.toLowerCase)
      case unhanded =>
        throw new IllegalArgumentException(s"Feed match type '$unhanded' was not recognized")
    }
  }

}
