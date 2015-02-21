Broadway
====
Broadway is a distributed actor-based processing server, and is optimized for high-speed data/file ingestion.

## Motivation

<a href="http://storm.apache.org/" target="new_window">Apache Storm</a> is a powerful and flexible distributed processing engine,
which is usually fed by a message-oriented middleware solution (like <a href="http://kafka.apache.org/" target="new_window">Apache Kafka</a>
or <a href="https://github.com/twitter/kestrel" target="new_window">Twitter Kestrel</a>). The challenge that I've identified,
is that organizations usually have to build a homegrown solution for the high-speed data/file ingestion into Kafka or Kestrel,
which distracts them from their core focus. I've built Broadway to help provide a solution to that challenge.

## About Broadway

As mentioned above, Broadway is a distributed actor-based processing server, and is optimized for high-speed data/file
ingestion. As such, Broadway is meant to be a complement to systems like Storm, and not necessarily an alternative.

Why the name Broadway? I chose the name Broadway (e.g. Broadway plays or musicals) because it's an actor-based system.
As such you'll encounter terms such as director, narrative and producer once Broadway's documentation is complete.

## Features

Broadway provides three main functions:

* *Transporting of Files* via a built-in orchestration server, which also has the capability to download files and/or moving files from one location (site) to another.
* *Extract, Transform and Loading* and is tailored toward processing flat files (XML, JSON, CSV, delimited, fixed field-length, and hierarchical)
* *File archival system*, which provides the capability for warehousing processed files.

Additionally, since Broadway is a file-centric processing system, it supports features like:
* File-processing dependencies (e.g. File "A" must be processed before Files "B" and "C" can be processed)
* File-processing schedulers and triggers
  * Directories can be watched for specific files (or using pattern matching) and then processed and archived.
  * Files can be limited to being processed at certain times or days of the week.
* An Actor-based I/O system with builtin support for:
  * Binary files
  * Text files (XML, JSON, CSV, delimited, fixed field-length, and hierarchical)
  * Kafka
* File archival and retention strategies
* Resource limits (e.g. limit the number of Kafka connections)

Broadway is currently pre-alpha quality software, and although it will currently run simple topologies, there's still
some work to do before it's ready for use by the general public. The current ETA is to have the system ready for action by
the end of March 2015.

## Build Requirements

* [Java SDK 1.7] (http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [Scala 2.11.4] (http://scala-lang.org/download/)
* [SBT 0.13+] (http://www.scala-sbt.org/download.html)

#### GitHub/ldaniels528 Dependencies

* [Trifecta 0.18.5] (https://github.com/ldaniels528/trifecta)
* [Tabular 0.1.0] (https://github.com/ldaniels528/tabular)

## How it works

Broadway provides a construct called a narrative (e.g. story), which describes the flow for a single processing event.
The proceeding example is a Broadway narrative that performs the following flow:

* Extracts stock symbols from a tabbed-delimited file.
* Retrieves stock quotes (via a custom service) for each symbol.
* Converts the stock quotes to <a href="http://avro.apache.org/" target="avro">Avro</a> records.
* Publishes each Avro record to a Kafka topic (shocktrade.quotes.yahoo.avro)

Below is the Broadway narrative that implements the flow described above:

```scala
class StockQuoteImportNarrative(config: ServerConfig) extends BroadwayNarrative(config, "Stock Quote Import")
with KafkaConstants {
  // create a file reader actor to read lines from the incoming resource
  val fileReader = addActor(new FileReadingActor(config))

  // create a Kafka publishing actor for stock quotes
  val quotePublisher = addActor(new KafkaAvroPublishingActor(quotesTopic, brokers))

  // create a stock quote lookup actor
  val quoteLookup = addActor(new StockQuoteLookupActor(quotePublisher))

  onStart { resource =>
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

Broadway provides an actor-based, event-based I/O system. Notice above, your code may react to messages that indicate
the opening (`OpeningFile`) or closing (`ClosingFile`) of a resource (e.g. file) or when a line of text has been
passed (`TextLine`).

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

And the narrative configuration is an XML file that describes how file feeds are mapped to narratives:

```xml
<narrative-config>

    <narrative id="QuoteImportNarrative" class="com.shocktrade.topologies.StockQuoteImportNarrative" />

    <location id="CSVQuotes" path="/Users/ldaniels/broadway/incoming/csvQuotes">
        <feed match="exact" name="AMEX.txt" narrative-ref="QuoteImportNarrative" />
        <feed match="exact" name="NASDAQ.txt" narrative-ref="QuoteImportNarrative" />
        <feed match="exact" name="NYSE.txt" narrative-ref="QuoteImportNarrative" />
        <feed match="exact" name="OTCBB.txt" narrative-ref="QuoteImportNarrative" />
    </location>

</narrative-config>
```

Broadway aims to provide maximum flexibility by offering two paths for defining narratives within a narrative configuration;
a Java/Scala-based narrative class or an XML-based narrative definition. The following is an example of a simple XML-based
narrative definition:

```xml
<etl-config name="NASDAQ">
    <template id="NasdaqTemplate">
        <field id="symbol" type="text"/>
        <field id="description" type="text"/>
    </template>

    <template id="ShockTradeTemplate">
        <field id="symbol" type="text"/>
        <field id="exchange" type="text"/>
        <field id="lastTrade" type="double"/>
        <field id="tradeDate" type="text"/>
        <field id="tradeTime" type="text"/>
        <field id="ask" type="double"/>
        <field id="bid" type="double"/>
        <field id="change" type="double"/>
        <field id="changePct" type="double"/>
        <field id="prevClose" type="double"/>
        <field id="open" type="double"/>
        <field id="close" type="double"/>
        <field id="high" type="double"/>
        <field id="low" type="double"/>
        <field id="volume" type="long"/>
        <field id="marketCap" type="double"/>
        <field id="errorMessage" type="text"/>
    </template>

    <service id="YFStockQuoteService"
             class="com.shocktrade.services.YFStockQuoteService"
             method="getQuote">
    </service>

    <!-- The orchestration will set the resource dynamically -->
    <input-source id="NasdaqSymbolsFile" template="NasdaqTemplate">
        <device type="file">
            <path type="dynamic" />
        </device>
    </input-source>

    <output-source id="OutputFile" template="ShockTradeTemplate">
        <device type="file">
            <path>/Users/ldaniels/nasdaq-flat.txt</path>
        </device>
    </output-source>

    <flow input="NasdaqSymbolsFile">

        <invoke service="YFStockQuoteService">
            <parameters>
                <param>{{ NasdaqSymbolsFile.symbol }}</param>
            </parameters>

            <on-response>
                <write-to device="OutputFile"/>
            </on-response>
        </invoke>
    </flow>
</etl-config>
```

**NOTE:** XML-based narrative definition support will be available in the next release.