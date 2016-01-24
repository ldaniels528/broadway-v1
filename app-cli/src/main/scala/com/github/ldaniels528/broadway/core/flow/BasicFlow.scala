package com.github.ldaniels528.broadway.core.flow

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.layout._

/**
  * Represents a basic ETL process flow implementation
  */
case class BasicFlow(id: String, input: InputDevice, output: OutputDevice, inputLayout: Layout, outputLayout: Layout)
  extends Flow with StatisticsGeneration {

  val devices = Seq(input, output)

  override def execute(rt: RuntimeContext) = {
    var inputData: Option[Data] = None
    do {
      // process the read
      inputData = input.read()
      inputData foreach (_ => updateCount(1))

      // and process the write
      val dataSet = inputLayout.in(rt, input, inputData)
      outputLayout.out(rt, output, dataSet, inputData.isEmpty) foreach { outputData =>
        updateCount(output.write(outputData))
      }

    } while (inputData.nonEmpty)
  }

}

