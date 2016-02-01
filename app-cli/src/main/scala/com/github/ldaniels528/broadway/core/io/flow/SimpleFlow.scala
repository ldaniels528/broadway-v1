package com.github.ldaniels528.broadway.core.io.flow

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.{AsynchronousOutputSource, InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a simple process flow implementation
  */
case class SimpleFlow(id: String, input: InputSource, output: OutputSource) extends Flow {

  override def devices = Seq(input, output)

  override def execute(scope: Scope)(implicit ec: ExecutionContext) = {
    implicit val myScope = scope

    input.open(scope)
    output.open(scope)

    var inputSet: Option[InputSet] = None
    do {
      // read the input record(s)
      inputSet = input.layout.read(input)

      // transform the output record(s)
      inputSet.filter(_.records.nonEmpty).foreach(output.layout.write(output, _))

    } while (inputSet.exists(!_.isEOF))

    input.close(scope)

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
