package com.github.ldaniels528.broadway.app.config

import java.io.{File, FileNotFoundException}

import com.github.ldaniels528.broadway.app.config.Directories._

import scala.language.postfixOps

/**
  * Server Directories
  * @author lawrence.daniels@gmail.com
  */
case class Directories(base: File) {
  val archive = new File(base, "archive") verify
  val completed = new File(base, "completed") verify
  val failed = new File(base, "failed") verify
  val incoming = new File(base, "incoming") verify
  val stories = new File(base, "stories") verify
  val work = new File(base, "work") verify

}

/**
  * Server Directories Companion Object
  */
object Directories {

  /**
    * File enrichment utility
    * @param file the given [[File file]]
    */
  implicit class FileEnrichment(val file: File) extends AnyVal {

    def verify: File = {
      if (!file.exists() && !file.mkdirs()) {
        throw new FileNotFoundException(s"File '${file.getAbsolutePath}' could not be accessed")
      }
      file
    }
  }

}