package com.github.ldaniels528.broadway.core.io.flow.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.{AsynchronousOutputSupport, InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.flow.Flow
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a simple process flow implementation
  * @author lawrence.daniels@gmail.com
  */
case class SimpleFlow(id: String, input: InputSource, output: OutputSource) extends Flow {

  override def devices = List(input, output).sortBy(_.id)

  override def execute(scope: Scope)(implicit ec: ExecutionContext) = {
    implicit val myScope = scope

    // open the input and output sources
    input.open
    output.open

    var inputSet: Option[InputSet] = None
    do {
      // read the input record(s)
      inputSet = input.layout.read(input)

      // transform the output record(s)
      inputSet.foreach(output.layout.write(output, _))

    } while (inputSet.exists(!_.isEOF))

    // close the input source, but not the output source as it might be asynchronous
    input.close

    // ask to be notified once all asynchronous writes have completed
    val task = output match {
      case aos: AsynchronousOutputSupport => aos.allWritesCompleted
      case _ => Future.successful({})
    }

    // close the output source once all writes have completed
    task onComplete (_ => output.close)
    task.map(_ => ())
  }

}
