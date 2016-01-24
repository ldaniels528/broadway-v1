package com.github.ldaniels528.broadway.core.io.layout.text

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.text.{TextReading, TextWriting}
import com.github.ldaniels528.broadway.core.io.layout._
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.language.postfixOps

/**
  * Text Layout
  */
case class TextLayout(id: String, header: Option[Division], body: Division, footer: Option[Division]) extends Layout {

  def in(rt: RuntimeContext, device: TextReading, line: Option[String]): Seq[Data] = {
    val data =
      if (header.exists(_.fieldSets.length >= device.offset)) None //header.map(_.fieldSets.map(fs => Data(fs.fields.map(f => rt.evaluate(f.name)))))
      else line.map(text => body.fieldSets.map(_.decode(text)))
    // TODO need to handle footer case

    data getOrElse Nil
  }

  def out(rt: RuntimeContext, device: TextWriting, dataSet: Seq[Data], isEOF: Boolean): Option[Int] = {
    val line_? =
      if (isEOF) footer.map(_.fieldSets.map(fs => fs.encode(Data(fs.fields.map(f => rt.evaluate(f.name))))))
      else {
        // if nothing has been written yet, generate the header if defined
        val headerData =
          if (device.offset == 0 && header.nonEmpty)
            header.map(_.fieldSets.map(fs => fs.encode(Data(fs.fields.map(f => rt.evaluate(f.name))))))
          else
            None

        val bodyData = dataSet.flatMap(data => body.fieldSets.map(_.encode(data)))
        headerData.map(_ ++ bodyData) ?? Option(bodyData)
      }

    val results = line_? map (_ map (line => device.writeLine(line)))
    results.map(_.sum)
  }

}
