package com.github.ldaniels528.broadway.core.io.layout.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.TextReadingSupport.TextInput
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.github.ldaniels528.broadway.core.io.layout.impl.MultiPartLayout._
import com.github.ldaniels528.broadway.core.io.record.Record

/**
  * Multi-Part Layout
  * @author lawrence.daniels@gmail.com
  */
case class MultiPartLayout(id: String, body: Section, header: Option[Section] = None, footer: Option[Section] = None) extends Layout {

  override def read(device: InputSource)(implicit scope: Scope) = {
    val textInput = device.readText
    val records = textInput match {
      // extract-only the optional header record(s)
      case Some(input) if isHeader(input) =>
        header.foreach(_.records.foreach(_.importText(input.line)))
        Nil

      // return the body record(s)
      case Some(input) => body.records.map(_.importText(input.line))

      // return the optional footer record(s)
      case None => footer.map(_.records) getOrElse Nil
    }

    InputSet(records, offset = device.getStatistics.offset, isEOF = textInput.isEmpty)
  }

  override def write(device: OutputSource, inputSet: InputSet)(implicit scope: Scope) {
    inputSet match {
      case is if is.isEOF => footer.foreach(_.records foreach device.writeRecord)
      case is =>
        if (device.getStatistics.offset == 0L) header.foreach(_.records foreach device.writeRecord)
        body.records foreach device.writeRecord
    }
  }

  private def isHeader(input: TextInput) = header.exists(_.records.length >= input.offset)

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

}