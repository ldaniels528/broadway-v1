package com.github.ldaniels528.broadway.core.flow

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.text.{TextReading, TextWriting}
import com.github.ldaniels528.broadway.core.io.device._

/**
  * Represents a basic ETL process flow implementation
  */
case class BasicFlow(id: String, input: InputDevice, output: OutputDevice)
  extends Flow with StatisticsGeneration {

  override def devices = Seq(input, output)

  override def execute(rt: RuntimeContext) = {
    input match {
      case device: TextReading => processTextReadAndWrite(device, rt)
      case device =>
        throw new IllegalArgumentException(s"Input device '${device.id}' is not supported")
    }
  }

  private def processTextReadAndWrite(device: TextReading, rt: RuntimeContext) {
    var line: Option[String] = None
    do {
      // process the read
      line = device.readLine
      line foreach (_ => updateCount(1))

      // and process the write
      val dataSet = device.layout.in(rt, device, line)
      processWrite(rt, dataSet, line.isEmpty) foreach updateCount

    } while (line.nonEmpty)
  }

  private def processWrite(rt: RuntimeContext, dataSet: Seq[Data], isEOF: Boolean) = {
    output match {
      case device: TextWriting => device.layout.out(rt, device, dataSet, isEOF)
      case device: BinaryWriting => device.layout.out(rt, device, dataSet, isEOF)
      case device =>
        throw new IllegalArgumentException(s"Unsupported device type '${device.id}'")
    }
  }

}

