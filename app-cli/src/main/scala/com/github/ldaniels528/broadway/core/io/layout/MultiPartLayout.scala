package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.github.ldaniels528.broadway.core.io.layout.MultiPartLayout.Section
import com.github.ldaniels528.broadway.core.util.ResourceHelper._

/**
  * Multi-Part Layout
  */
case class MultiPartLayout(id: String, body: Section, header: Option[Section] = None, footer: Option[Section] = None) extends Layout {

  override def read(device: InputSource)(implicit scope: Scope) = {
    val textInput = device.require[TextRecordInputSource](s"Text-capable input source required for device '${device.id}'").readLine(scope)
    val records = textInput match {
      // extract-only the optional header record(s)
      case Some(input) if header.exists(_.records.length >= input.offset) =>
        header.foreach(_.records.foreach(_.asInstanceOf[TextRecord].fromLine(input.line)))
        Nil

      // return the body record(s)
      case Some(input) =>
        body.records.map { record =>
          record.require[TextRecord](s"Illegal record type '${record.getClass.getSimpleName}'").fromLine(input.line)
        }

      // return the optional footer record(s)
      case None =>
        footer.map(_.records) getOrElse Nil
    }

    InputSet(records, offset = device.getStatistics(scope).offset, isEOF = textInput.isEmpty)
  }

  override def write(device: OutputSource, inputSet: InputSet)(implicit scope: Scope) {
    inputSet match {
      case is if is.isEOF => footer.foreach(_.records foreach device.writeRecord)
      case is =>
        if (device.getStatistics(scope).offset == 0L) header.foreach(_.records foreach device.writeRecord)
        body.records foreach device.writeRecord
    }
  }

}

/**
  * Multi-Part Layout Companion Object
  */
object MultiPartLayout {

  /**
    * Document Layout Section
    */
  case class Section(records: Seq[Record])

}