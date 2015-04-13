package com.ldaniels528.broadway.core.actors.file

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.BroadwayActor
import com.ldaniels528.broadway.core.actors.file.FileReadingActor.{BinaryBlock, TextLine}
import com.ldaniels528.broadway.core.resources.RandomAccessFileResource

/**
 * File Writing Actor
 * @param output the random access file for which to update
 */
class FileWritingActor(output: RandomAccessFileResource) extends BroadwayActor {
  override def receive = {
    case BinaryBlock(resource, offset, bytes) =>
      output.write(offset, bytes)
    case TextLine(resource, lineNo, line, tokens) =>
      if (lineNo > 1) output.write(line)
    case message =>
      unhandled(message)
  }
}
