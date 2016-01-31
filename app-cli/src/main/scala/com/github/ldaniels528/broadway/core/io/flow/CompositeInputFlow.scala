package com.github.ldaniels528.broadway.core.io.flow

import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.layout.Record
import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.{ExecutionContext, Future}

/**
  * Composite Input Flow
  */
case class CompositeInputFlow(id: String, theOutput: OutputSource, theInputs: Seq[InputSource]) extends Flow {
  val output = theOutput.asInstanceOf[TextRecordOutputSource]
  val inputs = theInputs.map(_.asInstanceOf[TextRecordInputSource])

  override def devices: Seq[DataSource] = inputs ++ Seq(output)

  override def execute(implicit scope: Scope, ec: ExecutionContext) = {
    output.open(scope)

    inputs foreach (_ use { input =>
      var inputRec: Option[Record] = None
      do {
        inputRec = input.readRecord(scope)

        // use the input layout to decide what the input data set should look like
        val inputSet = inputRec.toList // input.layout.in(scope, input, inputRec)

        // transform the input data set to an output data set, and write to persistence layer
        inputSet map (_.copyAs(output.templateRecord)) foreach output.writeRecord

      } while (inputRec.nonEmpty)
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