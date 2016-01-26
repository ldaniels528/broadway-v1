package com.github.ldaniels528.broadway.core.opcode.flow

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Represents a basic ETL process flow implementation
  */
case class BasicFlow(id: String, input: InputDevice, output: OutputDevice, inLayout: Layout, outLayout: Layout) extends Flow {

  val devices = Seq(input, output)

  override def execute(scope: Scope) = {
    var inputData: Option[Data] = None
    do {
      // process the read
      inputData = input.read(scope)

      // and process the write
      val dataSet = inLayout.in(scope, input, inputData)
      outLayout.out(scope, output, dataSet, inputData.isEmpty) foreach { outputData =>
        output.write(scope, data = outputData)
      }

    } while (inputData.nonEmpty)
  }

}

