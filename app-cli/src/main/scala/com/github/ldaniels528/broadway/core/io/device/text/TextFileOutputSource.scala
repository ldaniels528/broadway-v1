package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedWriter, File, FileWriter}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.scope.Scope
import org.slf4j.LoggerFactory

/**
  * Text File Output Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileOutputSource(id: String, path: String) extends OutputSource {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private var writer: Option[BufferedWriter] = None
  private val uuid = UUID.randomUUID().toString

  override def close(scope: Scope) {
    if (isGlobal) {
      //writer.foreach(_.close())
      //writer = None
    }
    else {
      scope.discardResource[BufferedWriter](uuid).foreach(_.close())
    }
  }

  override def open(scope: Scope) = {
    val file = new File(scope.evaluate(path))
    scope ++= Seq(
      "flow.output.filename" -> file.getName,
      "flow.output.lastModified" -> (() => file.lastModified()),
      "flow.output.length" -> (() => file.length()),
      "flow.output.path" -> file.getCanonicalPath
    )

    if (isGlobal) {
      if (writer.isEmpty)
        writer = Option(new BufferedWriter(new FileWriter(file)))
      else
        logger.warn(s"IOSource '$id' is already opened")
    }
    else {
      scope.createResource(uuid, new BufferedWriter(new FileWriter(file)))
    }
    ()
  }

  override def write(scope: Scope, data: Data) = {
    (if (isGlobal) writer else scope.getResource[BufferedWriter](uuid)) map (writeData(scope, _, data)) getOrElse 0
  }

  def isGlobal = !path.contains("{{")

  private def writeData(scope: Scope, out: BufferedWriter, data: Data) = {
    out.write(data.asText)
    out.newLine()
    updateCount(scope, 1)
  }

}
