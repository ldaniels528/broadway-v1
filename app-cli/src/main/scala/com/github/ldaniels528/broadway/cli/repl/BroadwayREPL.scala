package com.github.ldaniels528.broadway.cli.repl

import java.io.File

import com.github.ldaniels528.broadway.cli.repl.command.{CommandParser, UnixLikeArgs}
import com.github.ldaniels528.broadway.core.{StoryConfig, StoryProcessor}
import com.github.ldaniels528.tabular.Tabular
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Broadway ETL REPL
  */
object BroadwayREPL {
  private val logger = LoggerFactory.getLogger(getClass)
  private val Version = "0.12"
  private val out = System.out
  private val err = System.err
  private val tabular = new Tabular()
  private val etlProcessor = new StoryProcessor()

  // variables
  private var etlConfigs = TrieMap[String, StoryConfig]()
  //private var step: Option[StepThroughFlow] = None
  private var alive = true

  /**
    * For standalone execution
    *
    * @param args the given command line arguments
    */
  def main(args: Array[String]): Unit = shell(args)

  def shell(args: Array[String]): Unit = {
    logger.info(s"Broadway REPL v$Version")

    // if any files were specified at the command line, load them
    if (args.nonEmpty) {
      val configs = loadEtlConfigs(args)
      etlConfigs ++= configs
      configs foreach { case (_, config) =>
        etlProcessor.run(config)
      }
    } else {

      // start taking input form the user
      while (alive) {
        out.print("> ")
        val line = Console.in.readLine().trim
        if (line.nonEmpty) {
          try {
            interpret(line) match {
              case u: Unit =>
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

      // load "./app-cli/src/test/resources/eod_history_fixed.xml"
      case UnixLikeArgs(Some("load"), paths, _) =>
        etlConfigs ++= loadEtlConfigs(paths)
        ()

      // lscfg
      case UnixLikeArgs(Some("lscfg"), paths, _) =>
      //etlConfigs.values map (e => EtlConfigTx(name = e.id, flows = e.flows.size, devices = e.flows.flatMap(_.devices).distinct.size))

      // run "./app-cli/src/test/resources/eod_history_fixed.xml"
      case UnixLikeArgs(Some("run"), paths, _) =>
        paths foreach { path =>
          etlProcessor.run(new File(path))
        }

      // step
      case UnixLikeArgs(Some("step"), paths, _) =>
      /*
      step match {
        case Some(flow) =>
        case None => etlConfigs.values.headOption
      }*/

      case _ =>
        throw new IllegalArgumentException(s"Syntax error: $line")
    }
  }

  private def loadEtlConfigs(paths: Seq[String]) = {
    val results = paths flatMap { path =>
      val file = new File(path)
      etlProcessor.load(file) map { config =>
        (file.getCanonicalPath, config)
      }
    }
    logger.info(s"${results.length} ETL configurations loaded")
    results
  }

  case class EtlConfigTx(name: String, flows: Int, devices: Int)

}
