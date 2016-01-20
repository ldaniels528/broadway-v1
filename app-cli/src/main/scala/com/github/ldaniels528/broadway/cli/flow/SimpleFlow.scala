package com.github.ldaniels528.broadway.cli.flow

import com.github.ldaniels528.broadway.cli.EtlConfig
import com.github.ldaniels528.broadway.cli.io.device.{InputDevice, OutputDevice, StatisticsGeneration}

/**
  * Represents a Simple ETL process flow implementation
  */
case class SimpleFlow(id: String, input: InputDevice, output: OutputDevice) extends Flow with StatisticsGeneration {

  override def devices = Seq(input, output)

  override def execute(config: EtlConfig) = {
    updateCount(0)

    // add the header to the output source (if specified)
    output.layout.header foreach { header =>
      val data = header.generate(config)
      updateCount(output.write(data)) // TODO the fields specific to header and footer are being ignored
    }

    // process the input
    while (input.hasNext) {
      input.read() foreach { data =>
        updateCount(output.write(data))
      }
    }

    // add the footer to the output source (if specified)
    output.layout.footer foreach { footer =>
      val data = footer.generate(config)
      updateCount(output.write(data))
    }
  }

}
