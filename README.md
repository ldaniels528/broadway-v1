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
As such you'll encounter terms such as anthology, director, narrative and producer once Broadway's documentation is complete.

## Features

Broadway provides three main functions:

* *Transporting of Files* via a built-in orchestration server, which also has the capability to download files and/or move files from one location (site) to another.
* *Extract, Transform and Loading* and is tailored toward processing flat files (XML, JSON, CSV, delimited, fixed field-length, and hierarchical)
* *File archival system*, which provides the capability for warehousing processed files.

Additionally, since Broadway is a file-centric processing system, it supports features like:
* File-processing dependencies (e.g. File "A" must be processed before Files "B" and "C" can be processed)
* File-processing schedulers and triggers
  * Directories can be watched for specific file names (or match file names via regular expressions) which can then be processed and archived.
  * Files can be limited to being processed at certain times or days of the week.
* An Actor-based I/O system with builtin support for:
  * Binary files
  * Text files (XML, JSON, CSV, delimited, fixed field-length, and hierarchical)
  * Kafka (including Avro)
  * MongoDB
* File archival and retention strategies
* Resource limits (e.g. limit the number of Kafka connections)

Broadway is currently pre-alpha quality software, and although it will currently run simple topologies (anthologies), 
there's still some work to do before it's ready for use by the general public. The current ETA is to have the system 
ready for action by the end of May 2015.

## Build Requirements

* [Java SDK 1.7] (http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [Scala 2.11.4] (http://scala-lang.org/download/)
* [SBT 0.13+] (http://www.scala-sbt.org/download.html)

#### GitHub/ldaniels528 Dependencies

* [Trifecta 0.18.14+] (https://github.com/ldaniels528/trifecta)
* [Tabular 0.1.0] (https://github.com/ldaniels528/tabular)

## How it works

Broadway provides a construct called a narrative (e.g. story), which describes the flow for a single processing event.
The proceeding example is a Broadway narrative that performs the following flow:

* Extracts historical stock quotes from a tabbed-delimited file.
* Encodes the stock quotes as <a href="http://avro.apache.org/" target="avro">Avro</a> records.
* Publishes each Avro record to a Kafka topic (eoddata.tradinghistory.avro)

We'll start with the anthology, which is an XML file (comprised of one or more narratives) that describes the flow of 
the process; in this case, how file feeds are mapped to their respective processing endpoints (actors):

```xml
<anthology id="EodData" version="1.0">

    <!-- Narratives -->

    <narrative id="EodDataImportNarrative"
               class="com.shocktrade.datacenter.narratives.stock.eoddata.EodDataImportNarrative">
        <properties>
            <property key="kafka.topic">eoddata.tradinghistory.avro</property>
            <property key="kafka.topic.parallelism">5</property>
            <property key="mongo.database">shocktrade</property>
            <property key="mongo.replicas">dev801:27017,dev802:27017,dev803:27017</property>
            <property key="mongo.collection">Stocks</property>
            <property key="zookeeper.connect">dev801:2181</property>
        </properties>
    </narrative>

    <!-- Location Triggers -->

    <location id="tradingHistory" path="/Users/ldaniels/broadway/incoming/tradingHistory">
        <feed name="AMEX_(.*)[.]txt" match="regex" narrative-ref="EodDataImportNarrative"/>
        <feed name="NASDAQ_(.*)[.]txt" match="regex" narrative-ref="EodDataImportNarrative"/>
        <feed name="NYSE_(.*)[.]txt" match="regex" narrative-ref="EodDataImportNarrative"/>
        <feed name="OTCBB_(.*)[.]txt" match="regex" narrative-ref="EodDataImportNarrative"/>
    </location>

</anthology>
```

The following is the Broadway narrative that implements the flow described above:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<EtlConfig id="eod-history">
    <flows>
        <SimpleFlow id="EOD->CSV" input="input_file" output="output_file"/>
    </flows>

    <devices>
        <TextInputDevice id="input_file" path="./app-cli/src/test/resources/files/OTCBB_20121217.txt" layout="input_layout"/>
        <TextOutputDevice id="output_file" path="/tmp/otcbb_output.txt" layout="output_layout"/>
    </devices>

    <layouts>
        <Text id="input_layout">
            <header>
                <fields type="csv">
                    <field auto-trim="yes">&lt;ticker&gt;</field>
                    <field auto-trim="yes">&lt;date&gt;</field>
                    <field auto-trim="yes">&lt;open&gt;</field>
                    <field auto-trim="yes">&gt;high&gt;</field>
                    <field auto-trim="yes">&gt;low&gt;</field>
                    <field auto-trim="yes">&gt;close&gt;</field>
                    <field auto-trim="yes">&gt;vol&gt;</field>
                </fields>
            </header>
            <body>
                <fields type="csv">
                    <field auto-trim="yes">ticker</field>
                    <field auto-trim="yes">date</field>
                    <field auto-trim="yes">open</field>
                    <field auto-trim="yes">high</field>
                    <field auto-trim="yes">low</field>
                    <field auto-trim="yes">close</field>
                    <field auto-trim="yes">vol</field>
                </fields>
            </body>
        </Text>

        <Text id="output_layout">
            <header>
                <fields type="fixed-length">
                    <field length="10">ticker</field>
                    <field length="9">date</field>
                    <field length="12">open</field>
                    <field length="12">high</field>
                    <field length="12">low</field>
                    <field length="12">close</field>
                    <field length="12">vol</field>
                </fields>
            </header>
            <body>
                <fields type="fixed-length">
                    <field length="10">ticker</field>
                    <field length="9">date</field>
                    <field length="12">open</field>
                    <field length="12">high</field>
                    <field length="12">low</field>
                    <field length="12">close</field>
                    <field length="12">vol</field>
                </fields>
            </body>
            <footer>
                <fields type="fixed-length">
                    <field length="15">input</field>
                    <field length="10">{{ input_file.__OFFSET }}</field>
                    <field length="15">output</field>
                    <field length="10">{{ output_file.__OFFSET }}</field>
                </fields>
            </footer>
        </Text>
    </layouts>
</EtlConfig>
```
