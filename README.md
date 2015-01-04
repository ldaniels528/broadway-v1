Broadway
====
Broadway is a distributed actor-based processing server, and is optimized for high-speed data/file ingestion.

## Motivation

<a href="http://storm.apache.org/" target="new_window">Apache Storm</a> is a powerful and flexible distributed processing engine,
which is usually fed by a message-oriented middleware solution (like <a href="http://kafka.apache.org/" target="new_window">Apache Kafka</a>
or <a href="https://github.com/twitter/kestrel" target="new_window">Twitter Kestrel</a>). The challenge that I've identified,
is that organizations usually have to build a robust homegrown solution for the high-speed data/file ingestion into
Kafka or Kestrel. I've built Broadway to help provide a solution to that challenge.

## About Broadway

As mentioned above, Broadway is a distributed actor-based processing server, and is optimized for high-speed data/file
ingestion. Broadway is meant to be a complement to systems like Storm, not necessarily an alternative.

Why the name Broadway? I chose the name Broadway (as it Broadway plays or musicals) because it's an actor-based system.
As such you'll encounter terms such as director, narrative and producer once Broadway's documentation is complete.

Broadway provides three main functions:

* *Transporting of Files* via a builtin an orchestration server, which also has the capability to download files and/or moving files from one location (site) to another.
* *Extract Transform and Loading* and is tailored toward processing flat files (XML, JSON, CSV, delimited, fixed field-length, and hierarchical)
* *File archival system*, which provides the capability for warehousing files supplied by either the Data Transport or ETL modules.

Additionally, since Broadway is a file-centric processing system, it supports features like:
* File-processing dependencies (e.g. File "A" must be processed before Files "B" and "C" can be processed)
* File-processing schedulers and triggers
  * Directories can be watched for specific files (or using pattern matching) and then processed and archived.
  * Files can be limited to being processed at certain times or days of the week.
* An Actor-based I/O system with builtin support for:
  * Binary files
  * Text files (XML, JSON, CSV, delimited, fixed field-length, and hierarchical)
  * Kafka

Broadway is currently pre-alpha quality software, and although it will currently run simple topologies, there's still
some work to do before it's ready for use by the general public. The current ETA is to have the system ready for action by
the end of March 2015.

## How it works

Broadway provides a construct called a narrative (e.g. story), which describes the flow for a single processing event.
The proceeding example is a Broadway narrative performs the following flow:

* Extracts stock symbols from a tabbed-delimited file.
* Retrieves stock quotes (via a custom service) for each symbol.
* Converts the stock quotes to <a href="http://avro.apache.org/" target="avro">Avro</a> records.
* Publishes each Avro record to a Kafka topic (shocktrade.quotes.yahoo.avro)

Below is the Broadway topology that implements the flow described above:

```scala
class StockQuoteImportNarrative(config: ServerConfig) extends BroadwayNarrative(config, "Stock Quote Import")
  with KafkaConstants {

  onStart { resource =>

    implicit val ec = config.system.dispatcher

    // create a file reader actor to read lines from the incoming resource
    val fileReader = config.addActor(new FileReadingActor(config))

    // create a Kafka publishing actor for stock quotes
    val quotePublisher = config.addActor(new KafkaAvroPublishingActor(quotesTopic, brokers))

    // create a stock quote lookup actor
    val quoteLookup = config.addActor(new StockQuoteLookupActor(quotePublisher))

    // start the processing by submitting a request to the file reader actor
    fileReader ! CopyText(resource, quoteLookup, handler = Delimited("[\t]"))
  }
}
```

**NOTE:** The `KafkaAvroPublishingActor` and `FileReadingActor` actors are builtin components of Broadway.

The class below is an optional custom actor that will perform the stock symbol look-ups and then pass an Avro-encoded
record to the Kafka publishing actor (a built-in component).

```scala
class StockQuoteLookupActor(target: BWxActorRef)(implicit ec: ExecutionContext) extends Actor {
  private val parameters = YFStockQuoteService.getParams(
    "symbol", "exchange", "lastTrade", "tradeDate", "tradeTime", "ask", "bid", "change", "changePct",
    "prevClose", "open", "close", "high", "low", "volume", "marketCap", "errorMessage")

  override def receive = {
    case OpeningFile(resource) =>
      ResourceTracker.start(resource)

    case ClosingFile(resource) =>
      ResourceTracker.stop(resource)

    case TextLine(resource, lineNo, line, tokens) =>
      tokens.headOption foreach { symbol =>
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

The following code needs no explanation, it's simply a collection of Kafka constants that could have just as easily
been placed in a properties file.

```scala
trait KafkaConstants {
  val eodDataTopic = "shocktrade.eoddata.yahoo.avro"
  val keyStatsTopic = "shocktrade.keystats.yahoo.avro"
  val quotesTopic = "shocktrade.quotes.yahoo.avro"

  val zkHost = "dev501:2181"
  val brokers = "dev501:9091,dev501:9092,dev501:9093,dev501:9094,dev501:9095,dev501:9096"
}
```

And an XML file to describe how files will be mapped to the narrative:

```xml
<narrative-config>

    <topology id="QuoteImportTopology" class="com.shocktrade.topologies.StockQuoteImportNarrative" />

    <location id="CSVQuotes" path="/Users/ldaniels/broadway/incoming/csvQuotes">
        <feed match="exact" name="AMEX.txt" topology-ref="QuoteImportTopology" />
        <feed match="exact" name="NASDAQ.txt" topology-ref="QuoteImportTopology" />
        <feed match="exact" name="NYSE.txt" topology-ref="QuoteImportTopology" />
        <feed match="exact" name="OTCBB.txt" topology-ref="QuoteImportTopology" />
    </location>

</narrative-config>
```

