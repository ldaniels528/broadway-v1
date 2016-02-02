package com.github.ldaniels528.broadway.core.io.flow.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.{AsynchronousOutputSupport, InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.flow.Flow
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Composite Output Flow
  * @author lawrence.daniels@gmail.com
  */
case class CompositeOutputFlow(id: String, input: InputSource, outputs: Seq[OutputSource]) extends Flow {

  override def devices = (input :: outputs.toList).sortBy(_.id)

  override def execute(scope: Scope)(implicit ec: ExecutionContext) = {
    implicit val myScope = scope

    // open the input and output sources
    input.open
    outputs foreach (_.open)

    // cycle through the input data
    var inputSet: Option[InputSet] = None
    do {
      // read the input record(s)
      inputSet = input.layout.read(input)

      // transform the output record(s)
      outputs foreach { output =>
        inputSet.filter(_.records.nonEmpty).foreach(output.layout.write(output, _))
      }

    } while (inputSet.exists(!_.isEOF))

    // close the input source
    input.close

    // ask to be notified once all asynchronous writes have completed
    val task = Future.sequence(outputs flatMap {
      case aos: AsynchronousOutputSupport => Some(aos.allWritesCompleted)
      case _ => None
    })

    // close the output source once all writes have completed
    task onComplete (_ => outputs.foreach(_.close))
    task.map(_ => ())
  }

}