package com.github.ldaniels528.broadway.core.io.flow

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a basic process flow implementation
  */
case class BasicFlow(id: String, input: InputSource, output: OutputSource) extends Flow {

  val devices = Seq(input, output)

  override def execute(implicit scope: Scope, ec: ExecutionContext) = {
    output.open(scope)

    input use { in =>
      var inputData: Option[Data] = None
      do {
        // process the read
        inputData = in.read(scope)

        // use the input layout to decide what the input data set should look like
        val dataSet = in.layout.in(scope, in, inputData)

        // transform the input data set to an output data set, and write to persistence layer
        output.layout.out(scope, output, dataSet, inputData.isEmpty) foreach { outputData =>
          output.write(scope, data = outputData)
        }

      } while (inputData.nonEmpty)
    }

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

