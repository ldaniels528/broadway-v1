package com.github.ldaniels528.broadway.cli

import java.io.File
import java.util.UUID

import com.github.ldaniels528.broadway.cli.command.{CommandParser, UnixLikeArgs}
import com.github.ldaniels528.broadway.core.{StoryConfig, StoryProcessor}
import com.github.ldaniels528.tabular.Tabular
import com.ldaniels528.commons.helpers.OptionHelper._
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

/**
  * Broadway ETL REPL
  * @author lawrence.daniels@gmail.com
  */
object BroadwayREPL {
  private val logger = LoggerFactory.getLogger(getClass)
  private val Version = "0.12"
  private val out = System.out
  private val err = System.err
  private val tabular = new Tabular()
  private val processor = new StoryProcessor()

  // variables
  private var stories = TrieMap[String, StoryConfig]()
  private var dataFiles = TrieMap[UUID, DataFile]()
  private var alive = true

  /**
    * For standalone execution
    * @param args the given command line arguments
    */
  def main(args: Array[String]): Unit = shell(args)

  def shell(args: Array[String]): Unit = {
    logger.info(s"Broadway REPL v$Version")

    // if any files were specified at the command line, load them
    if (args.nonEmpty) {
      val configs = loadStory(args)
      stories ++= configs
      configs foreach { case (_, config) =>
        processor.run(config)
      }
    } else {

      // start taking input form the user
      while (alive) {
        out.print("> ")
        val line = Console.in.readLine().trim
        if (line.nonEmpty) {
          try {
            interpret(line) match {
              case u: Unit => out.println("Ok")
              case results: Array[_] => tabular.transform(results) foreach out.println
              case results: Iterable[_] => tabular.transform(results.toSeq) foreach out.println
              case results: Seq[_] => tabular.transform(results) foreach out.println
              case result =>
                out.println(result)
            }
          } catch {
            case e: Exception =>
              err.println(e.getMessage)
          }
        }
      }
    }
  }

  private def interpret(line: String): Any = {
    val command = CommandParser.parseUnixLikeArgs(line)
    //logger.info(s"command = $command")

    command match {
      // exit
      case UnixLikeArgs(Some("exit"), _, _) => alive = false

      // loadcfg "./app-cli/src/test/resources/eod_history_fixed.xml"
      case UnixLikeArgs(Some("loadcfg"), paths, _) =>
        stories ++= loadStory(paths)
        ()

      // loadfile "./app-cli/src/test/resources/files/DownloadAllNumbers.txt"
      case UnixLikeArgs(Some("loadfile"), paths, _) =>
        dataFiles ++= paths map (path => DataFile(UUID.randomUUID(), new File(path), Source.fromFile(path).getLines())) map (df => df.uuid -> df)
        ()

      // lscfg
      case UnixLikeArgs(Some("lscfg"), paths, _) =>
        stories.values map (sc => StoryInfo(name = sc.id, triggers = sc.triggers.size, properties = sc.properties.size))

      // lsfiles
      case UnixLikeArgs(Some("lsfiles"), paths, _) =>
        dataFiles.map { case (uuid, df) =>
          DataFileInfo(uuid, df.file.getName, df.file.length())
        }

      // nextline
      case UnixLikeArgs(Some("nextline"), uuids, _) =>
        uuids.map(UUID.fromString) map { uuid =>
          val df = dataFiles.get(uuid) orDie s"No data file handle found for '$uuid'"
          val line = df.lines.next()
          df.offset += 1
          Data(uuid, df.offset, decode(line))
        }

      // runcfg "./app-cli/src/test/resources/eod_history_fixed.xml"
      case UnixLikeArgs(Some("runcfg"), paths, _) =>
        paths foreach { path =>
          processor.run(new File(path))
        }

      case _ =>
        throw new IllegalArgumentException(s"Syntax error: $line")
    }
  }

  private def loadStory(paths: Seq[String]) = {
    val results = paths flatMap { path =>
      val file = new File(path)
      processor.load(file) map { config =>
        (file.getCanonicalPath, config)
      }
    }
    logger.info(s"${results.length} stories configurations loaded")
    results
  }

  private def decode(s: String) = {
    s.replaceAllLiterally("\t", "[TAB]").replaceAllLiterally("\r", "[CR]").replaceAllLiterally("\n", "[LF]")
  }

  case class StoryInfo(name: String, triggers: Int, properties: Int)

  case class Data(uuid: UUID, offset: Long, line: String)

  case class DataFile(uuid: UUID, file: File, lines: Iterator[String], var offset: Long = 0)

  case class DataFileInfo(uuid: UUID, name: String, size: Long)

}
