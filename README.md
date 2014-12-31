Broadway
====

Broadway is a distributed actor-based processing server, and is optimized for high-speed data/file ingestion. Broadway is composed
of 3 main modules:

* Data Transporter - an orchestration server, which is responsible for download files and/or moving files from one location (site) to another.
* ETL - an Extract Transform and Loading system
* DataStore - a file archival system, which is responsible for warehousing files supplied by either the Data Transport or ETL modules.

## About Broadway

Broadway is being designed for a very specific processing use case... High speed file ingestion. And while Broadway will
share many similarities with existing processing engines, like <a href="http://storm.apache.org/" target="storm">Apache Storm</a>,
it is not intended as a replacement for Storm or other stream-oriented processing systems. Broadway is targeting
environments where many different individual files require processing, while Storm excels at processing streams of data
but is not specifically geared toward the processing one-off files.

## Getting the Code

Broadway is currently pre-alpha quality software, and although it will currently run simple topologies, there's still
some work to do before it can be made available to the general public. The current ETA is to have the code ready for
action by the end of January 2015.

## Creating a Broadway Topology

The proceeding example is a Broadway topology performs the following flow:

* Extracts stock symbols from a tabbed-delimited file.
* Retrieves stock quotes for each symbol.
* Converts the stock quotes to <a href="http://avro.apache.org/" target="avro">Avro</a> records.
* Publishes each Avro record to a Kafka topic (shocktrade.quotes.yahoo.avro)

Below is the Broadway topology that implements the flow described above:

```scala
class NASDAQSymbolImportTopology() extends BroadwayTopology("NASDAQ Symbol Import Topology") {
  private val topic = "shocktrade.quotes.yahoo.avro"
  private val brokers = "dev501:9091,dev501:9092,dev501:9093,dev501:9094,dev501:9095,dev501:9096"

  onStart { resource =>
    // create a Kafka publisher actor
    val kafkaPublisher = addActor(new KafkaAvroPublisher(topic, brokers))

    // create a stock quote lookup actor
    val quoteLookup = addActor(new StockQuoteLookupActor(kafkaPublisher))

    // create a file reader actor to read lines from the incoming resource
    val fileReader = addActor(new TextFileReader())

    // start the processing by submitting a request to the file reader actor
    fileReader ! DelimitedFile(resource, "\t", quoteLookup)
  }
}
```

**NOTE:** The `KafkaAvroPublisher` and `TextFileReader` actors are builtin components of Broadway.

The class below is an optional custom actor that will perform the stock symbol look-ups and then pass an Avro-encoded
record to the Kafka publishing actor (a built-in component).

```scala
class StockQuoteLookupActor(target: ActorRef)(implicit ec: ExecutionContext) extends Actor {
  private val parameters = YFStockQuoteService.getParams(
    "symbol", "exchange", "lastTrade", "tradeDate", "tradeTime", "ask", "bid",
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
      unhandled(message)
  }
}
```

And an XML file to describe how files will be mapped to the topology:

```xml
<?xml version="1.0" ?>
<topology class="com.shocktrade.topologies.NASDAQSymbolImportTopology">
    <feed match="exact">AMEX.txt</feed>
    <feed match="exact">NASDAQ.txt</feed>
    <feed match="exact">NYSE.txt</feed>
    <feed match="exact">OTCBB.txt</feed>
</topology>
```
