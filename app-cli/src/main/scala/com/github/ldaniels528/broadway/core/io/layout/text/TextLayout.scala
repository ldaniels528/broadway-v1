package com.github.ldaniels528.broadway.core.io.layout.text

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputDevice, OutputDevice}
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.scope.Scope
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.language.postfixOps

/**
  * Text Layout
  */
case class TextLayout(id: String, header: Option[Division], body: Division, footer: Option[Division]) extends Layout {

  override def in(scope: Scope, device: InputDevice, data: Option[Data]): Seq[Data] = {
    val result =
      if (header.exists(_.fieldSets.length >= device.offset)) None //header.map(_.fieldSets.map(fs => Data(fs.fields.map(f => scope.evaluate(f.name)))))
      else data.map(_.asText).map(text => body.fieldSets.map(_.decode(text)))
    // TODO need to handle footer case

    result getOrElse Nil
  }

  override def out(scope: Scope, device: OutputDevice, dataSet: Seq[Data], isEOF: Boolean): Seq[Data] = {
    val line_? =
      if (isEOF) footer.map(_.fieldSets.map(fs => Data(fs, fs.encode(Data(fs, fs.fields.map(f => scope.evaluate(f.name)))))))
      else {
        // if nothing has been written yet, generate the header if defined
        val headerData =
          if (device.offset == 0 && header.nonEmpty)
            header.map(_.fieldSets.map(fs => Data(fs, fs.encode(Data(fs, fs.fields.map(f => scope.evaluate(f.name)))))))
          else
            None

        val bodyData = dataSet.flatMap(data => body.fieldSets.map(fs => Data(fs, fs.encode(data))))
        headerData.map(_ ++ bodyData) ?? Option(bodyData)
      }

    line_? getOrElse Nil
  }

}
