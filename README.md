Broadway
====

Broadway is a distributed actor-based processing server, and is optimized for high-speed data/file ingestion. Broadway is composed
of 3 main modules:

* Data Transporter - an orchestration server, which is responsible for download files and/or moving files from one location (site) to another.
* ETL - an Extract Transform and Loading system
* DataStore - a file archival system, which is responsible for warehousing files supplied by either the Data Transport or ETL modules.

## About Broadway

Broadway is being designed for a very specific processing use case... High speed file ingestion, which is different than
tools like <a href="http://storm.apache.org/" target="storm">Apache Storm</a>, which excels at processing streams of data
but is not specifically geared toward processing one-off files.

## Getting the Code

Broadway is currently pre-alpha quality software, and although it will currently run simple topologies, there's still
some work to do before it can be made available to the general public. The current ETA is to have the code ready for
action by the end of January 2015.

## Creating a Topology

The following code demonstrates how to create a Broadway topology:

    val topology = new BroadwayTopology("NASDAQ Data Topology")
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

