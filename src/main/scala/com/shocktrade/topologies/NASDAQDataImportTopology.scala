package com.shocktrade.topologies

import akka.actor.{Actor, ActorRef, Props}
import com.ldaniels528.broadway.server.etl.BroadwayTopology
import com.ldaniels528.broadway.server.etl.actors.TextFileReader.DelimitedFile
import com.ldaniels528.broadway.server.etl.actors.{KafkaAvroPublisher, TextFileReader}
import com.ldaniels528.trifecta.io.avro.AvroConversion
import com.shocktrade.services.{YFStockQuoteService, YahooFinanceServices}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
 * ShockTrade NASDAQ Data Import Topology
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object NASDAQDataImportTopology {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  val topic = "shocktrade.quotes.yahoo.avro"
  val brokers = "dev501:9091,dev501:9092,dev501:9093,dev501:9094,dev501:9095,dev501:9096"

  def createTopology(fileName: String) = {
    val topology = new BroadwayTopology(s"NASDAQ Topology ($fileName)")
    topology.onStart { resource =>
      import topology.executionContext

      // create a Kafka publisher actor
      val kafkaPublisher = topology.system.actorOf(Props(new KafkaAvroPublisher(topic, brokers)))

      // create a stock quote lookup actor
      val quoteLookup = topology.system.actorOf(Props(new StockQuoteLookupActor(kafkaPublisher)))

      // create a file reader actor to read lines from the incoming resource
      val fileReader = topology.system.actorOf(Props(new TextFileReader()))

      // start the processing by submitting a request to the file reader actor
      fileReader ! DelimitedFile(resource, "\t", quoteLookup)
    }
    topology
  }

  /**
   * Stock Quote Lookup Actor
   * @author Lawrence Daniels <lawrence.daniels@gmail.com>
   */
  class StockQuoteLookupActor(target: ActorRef)(implicit ec: ExecutionContext) extends Actor {
    private lazy val logger = LoggerFactory.getLogger(getClass)
    private val parameters = YFStockQuoteService.getParams(
      "symbol", "exchange", "lastTrade", "tradeDate", "tradeTime", "ask", /*"askSize",*/ "bid", /*"bidSize",*/
      "change", "changePct", "prevClose", "open", "close", "high", "low", "volume", "marketCap", "errorMessage")

    override def receive = {
      case symbolData: Array[String] =>
        symbolData.headOption foreach { symbol =>
          YahooFinanceServices.getStockQuote(symbol, parameters) foreach { quote =>
            val builder = com.shocktrade.avro.CSVQuoteRecord.newBuilder()
            AvroConversion.copy(quote, builder)
            target ! builder.build()
          }
        }
      case message =>
        logger.warn(s"Unhandled message $message")
        unhandled(message)
    }
  }

}
