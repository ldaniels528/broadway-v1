package com.github.ldaniels528.broadway.core.flow

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputDevice, OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.Division

/**
  * Represents a Simple ETL process flow implementation
  */
case class SimpleFlow(id: String, input: InputDevice, output: OutputDevice)
  extends Flow with StepThroughFlow with StatisticsGeneration {

  override def devices = Seq(input, output)

  override def execute(rt: RuntimeContext) = {
    updateCount(0)

    // add the header to the output source (if specified)
    firstStep(rt)

    // process the input
    while (input.hasNext) {
      input.read() foreach { data =>
        updateCount(output.write(data))
      }
    }

    // add the footer to the output source (if specified)
    lastStep(rt)
  }

  override def firstStep(rt: RuntimeContext) = generateDivisions(rt, output.layout.header)

  override def nextStep(config: RuntimeContext) = {
    val hasNext = input.hasNext
    if (hasNext) {
      input.read() foreach { data =>
        updateCount(output.write(data))
      }
    }
    hasNext
  }

  override def lastStep(rt: RuntimeContext) = generateDivisions(rt, output.layout.footer)

  private def generateDivisions(rt: RuntimeContext, divisions: Seq[Division]) = {
    divisions foreach { division =>
      val data = Data(division.fieldSet.fields.map(f => rt.evaluate(f.name)))
      updateCount(output.write(data)) // TODO the fields specific to this division are being ignored
    }
  }

}
