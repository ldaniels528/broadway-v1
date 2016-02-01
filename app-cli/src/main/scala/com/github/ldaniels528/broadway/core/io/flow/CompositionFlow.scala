package com.github.ldaniels528.broadway.core.io.flow

import com.github.ldaniels528.broadway.core.io.device.DataSource._
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.{Data, Scope}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Composite Input Flow
  */
@deprecated(message = "Use CompositeInputFlow instead")
case class CompositionFlow(id: String, output: OutputSource, inputs: Seq[InputSource]) extends Flow {

  override def devices: Seq[DataSource] = inputs ++ Seq(output)

  override def execute(scope: Scope)(implicit ec: ExecutionContext) = {
    implicit val myScope = scope

    output.open(scope)

    inputs foreach (_ use { input =>
      var inputData: Option[Data] = None
      do {
        inputData = input.read(scope)

        // use the input layout to decide what the input data set should look like
        val dataSet = input.layout.in(scope, input, inputData)

        // transform the input data set to an output data set, and write to persistence layer
        output.layout.out(scope, output, dataSet, inputData.isEmpty) foreach { outputData =>
          output.write(scope, data = outputData)
        }

      } while (inputData.nonEmpty)
    })

    // wait for all asynchronous writes to complete
    val task = output match {
      case aos: AsynchronousOutputSource => aos.allWritesCompleted
      case _ => Future.successful({})
    }

    // close the output source once all writes have completed
    task onComplete (_ => output.close(scope))
    task
  }

}