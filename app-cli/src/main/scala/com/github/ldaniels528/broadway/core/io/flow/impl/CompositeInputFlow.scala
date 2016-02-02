package com.github.ldaniels528.broadway.core.io.flow.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.flow.Flow
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Composite Input Flow
  * @author lawrence.daniels@gmail.com
  */
case class CompositeInputFlow(id: String, output: OutputSource, inputs: Seq[InputSource]) extends Flow {

  override def devices = (output :: inputs.toList).sortBy(_.id)

  override def execute(scope: Scope)(implicit ec: ExecutionContext) = {
    implicit val myScope = scope

    // open the output source, which will remain open throughout the process
    output.open

    // cycle through the input sources
    inputs foreach (_ use { input =>
      var inputSet: Option[InputSet] = None
      do {
        // read the input record(s)
        inputSet = input.layout.read(input)

        // transform the output record(s)
        inputSet.filter(_.records.nonEmpty).foreach(output.layout.write(output, _))

      } while (inputSet.exists(!_.isEOF))
    })

    // ask to be notified once all asynchronous writes have completed
    val task = Future.sequence((output match {
      case aos: AsynchronousOutputSupport => Some(aos.allWritesCompleted)
      case _ => None
    }).toList)

    // close the output source once all writes have completed
    task onComplete (_ => output.close)
    task.map(_ => ())
  }

}