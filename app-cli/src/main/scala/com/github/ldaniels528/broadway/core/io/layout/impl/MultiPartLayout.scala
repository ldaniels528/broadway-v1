package com.github.ldaniels528.broadway.core.io.layout.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.TextReadingSupport.TextInput
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.github.ldaniels528.broadway.core.io.layout.impl.MultiPartLayout._
import com.github.ldaniels528.broadway.core.io.record.Record

import scala.collection.concurrent.TrieMap

/**
  * Multi-Record Layout implementation
  * @author lawrence.daniels@gmail.com
  */
case class MultiPartLayout(id: String, body: Section, header: Option[Section], trailer: Option[Section]) extends Layout {
  private val buffers = TrieMap[InputSource, List[TextInput]]()

  override def read(device: InputSource)(implicit scope: Scope) = {
    val textInput = readLine(device)
    val records = textInput match {
      // extract-only the optional header record(s)
      case Some(input) if input.isHeader =>
        header.foreach(_.records.foreach(_.importText(input.textInput.line)))
        Nil

      // extract-only the optional trailer record(s)
      case Some(input) if input.isTrailer =>
        trailer.foreach(_.records.foreach(_.importText(input.textInput.line)))
        Nil

      // extract & populate the body record(s)
      case Some(input) =>
        body.records.map(_.importText(input.textInput.line))

      // end-of-files
      case None =>
        buffers.clear()
        Nil
    }

    InputSet(records, offset = device.getStatistics.offset, isEOF = textInput.isEmpty)
  }

  override def write(device: OutputSource, inputSet: InputSet)(implicit scope: Scope) {
    inputSet match {
      case is if is.isEOF => trailer.foreach(_.records foreach device.writeRecord)
      case is =>
        if (device.getStatistics.offset == 0L) header.foreach(_.records foreach device.writeRecord)
        body.records foreach device.writeRecord
    }
  }


  private def readLine(device: InputSource)(implicit scope: Scope) = {
    var buffer = buffers.getOrElseUpdate(device, Nil)
    var eof = false

    // make sure the head ahead buffer is filled
    var newLines: List[TextInput] = Nil
    while (!eof && buffer.size < readAhead) {
      val text = device.readText
      eof = text.isEmpty
      text.foreach(ti => newLines = ti :: newLines)
    }
    buffer = buffer ::: newLines.reverse

    // return the next line from the buffer
    val input = buffer.headOption
    buffer = if (buffer.nonEmpty) buffer.tail else Nil
    buffers(device) = buffer
    input.map(ti => InputData(
      textInput = ti,
      buffer = buffer,
      isHeader = header.exists(_.records.length >= ti.offset),
      isTrailer = trailer.exists(_.records.length > buffer.length)))
  }

  private def readAhead: Int = {
    1 + (header.map(_.records.length) getOrElse 0) + (trailer.map(_.records.length) getOrElse 0)
  }

}

/**
  * Multi-Part Layout Companion Object
  * @author lawrence.daniels@gmail.com
  */
object MultiPartLayout {

  /**
    * Document Layout Section
    */
  case class Section(records: Seq[Record])

  case class InputData(textInput: TextInput, buffer: List[TextInput] = Nil, isHeader: Boolean, isTrailer: Boolean)

}