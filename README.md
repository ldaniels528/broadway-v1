Broadway
========
Broadway is a distributed actor-based processing server, and is optimized for high-speed data/file ingestion.

## Motivation

Systems like [Apache Storm](http://storm.apache.org) or [Spark Streaming](http://spark.apache.org) are powerful and flexible distributed processing engines,
which are usually fed by a message-oriented middleware solution (e.g. [Apache Kafka](http://kafka.apache.org) or [Twitter Kestrel](https://github.com/twitter/kestrel)). 

The challenge that I've identified, is that organizations usually have to build a homegrown solution for the high-speed 
data/file ingestion into Kafka or Kestrel, which distracts them from their core focus. I've built Broadway to help provide 
a solution to that challenge.

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

<a name="build-requirements"></a>
## Build Requirements

* [Java SDK 1.7] (http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [Scala 2.11.4] (http://scala-lang.org/download/)
* [SBT 0.13+] (http://www.scala-sbt.org/download.html)

<a name="external-dependencies"></a>
### External Dependencies

* [Commons-Helpers 0.1.2] (https://github.com/ldaniels528/commons-helpers)
* [ScalaScript 0.2.20] (https://github.com/ldaniels528/scalascript)
* [Tabular 0.1.3] (https://github.com/ldaniels528/tabular)

<a name="how-it-works"></a>
## How it works

Broadway provides a construct called a narrative (e.g. story), which describes the flow for a single processing event.
The proceeding example is a Broadway narrative that performs the following flow:

* Extracts historical stock quotes from a tabbed-delimited file.
* Encodes the stock quotes as <a href="http://avro.apache.org/" target="avro">Avro</a> records.
* Publishes each Avro record to a Kafka topic (eoddata.tradinghistory.avro)

We'll start with the story configuration, which is an XML file (comprised of one or more narratives) that 
describes the flow of the process; in this case, how file feeds are mapped to their respective processing 
endpoints (actors):

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<story id="eod_companies_csv">
    <import path="./app-cli/src/test/resources/global_settings.xml" />

    <triggers>
        <StartUpTrigger id="File_Combining_Trigger">
            <CompositeFlow id="combiner_flow">
                <input-sources>
                    <include source="AMEX" />
                    <include source="NASDAQ" />
                    <include source="NYSE" />
                    <include source="OTCBB" />
                </input-sources>
                <output-sources>
                    <include source="output_csv" />
                    <include source="output_fixed" />
                    <include source="output_json" />
                </output-sources>
            </CompositeFlow>
        </StartUpTrigger>
    </triggers>

    <data-sources>
        <TextFileInputSource id="AMEX" path="./app-cli/src/test/resources/files/AMEX.txt" layout="eod_company_input_layout" />
        <TextFileInputSource id="NASDAQ" path="./app-cli/src/test/resources/files/NASDAQ.txt" layout="eod_company_input_layout" />
        <TextFileInputSource id="NYSE" path="./app-cli/src/test/resources/files/NYSE.txt" layout="eod_company_input_layout" />
        <TextFileInputSource id="OTCBB" path="./app-cli/src/test/resources/files/OTCBB.txt" layout="eod_company_input_layout" />

        <TextFileOutputSource id="output_csv" path="{{ java.io.tmpdir }}/eod_companies_csv.txt" layout="csv_layout" />
        <TextFileOutputSource id="output_fixed" path="{{ java.io.tmpdir }}/eod_companies_fixed.txt" layout="fixed_layout" />
        <TextFileOutputSource id="output_json" path="{{ java.io.tmpdir }}/eod_companies_json.txt" layout="json_layout" />
    </data-sources>

    <layouts>
        <MultiPartLayout id="csv_layout">
            <header>
                <record id="cvs_header" format="csv">
                    <field name="exchange" type="string" value="Exchange" />
                    <field name="symbol" type="string" value="Ticker" />
                    <field name="description" type="string" value="Description" />
                    <field name="source" type="string" value="Source" />
                    <field name="lineNo" type="string" value="Line Number" />
                </record>
            </header>
            <body>
                <record id="csv_data" format="csv">
                    <field name="exchange" type="string">{{ flow.input.id }}</field>
                    <field name="symbol" type="string">{{ symbol }}</field>
                    <field name="description" type="string">{{ description }}</field>
                    <field name="source" type="string">{{ flow.input.filename }}</field>
                    <field name="lineNo" type="int">{{ flow.input.offset }}</field>
                </record>
            </body>
        </MultiPartLayout>

        <MultiPartLayout id="fixed_layout">
            <header>
                <record id="fixed_header" format="fixed">
                    <field name="symbol" value="Symbol" type="string" length="10" />
                    <field name="description" value="Description" type="string" length="50"/>
                    <field name="source" type="string" value="Source" length="40" />
                    <field name="lineNo" type="string" value="Line Number" length="12" />
                </record>
            </header>
            <body>
                <record id="fixed_body" format="fixed">
                    <field name="symbol" type="string" length="10" value="{{ symbol }}"/>
                    <field name="description" type="string" length="50" value="{{ description }}"/>
                    <field name="source" type="string" length="40" value="{{ flow.input.filename }}" />
                    <field name="lineNo" type="int" length="12" value="{{ flow.input.offset }}" />
                </record>
            </body>
        </MultiPartLayout>

        <MultiPartLayout id="json_layout">
            <body>
                <record id="json_body" format="json">
                    <field name="symbol" type="string" value="{{ symbol }}" />
                    <field name="description" type="string" value="{{ description }}" />
                </record>
            </body>
        </MultiPartLayout>
    </layouts>
</story>
```

Broadway provides a number of options for flow control. In the example above, we've defined a composition of 
input sources (```AMEX```, ```NASDAQ```, ```NYSE``` and ```OTCBB```), which are written to a collection of output 
sources in the format prescribed by each source. 

Thus, the output source "output_csv" will create a CSV representation of the data:
```
"Exchange","Ticker","Description","Source","Line Number"
"AMEX","AA.P","Alcoa Inc Pf 3.75","AMEX.txt",2
"AMEX","AADR","BNY Mellon Focused Growth ADR ETF","AMEX.txt",3
```
 
The output source "output_fixed" will create a fixed-length representation of the data: 
```
Symbol    Description                                       Source                                  Line Number 
AA.P      Alcoa Inc Pf 3.75                                 AMEX.txt                                2           
AADR      BNY Mellon Focused Growth ADR ETF                 AMEX.txt                                3                  
```

And finally, the output source "output_json" will create a JSON representation of the data:
```json
{"symbol":"AA.P","description":"Alcoa Inc Pf 3.75"}
{"symbol":"AADR","description":"BNY Mellon Focused Growth ADR ETF"}
{"symbol":"AAMC","description":"Altisource Asset"}
```

Alternatively, we could have defined a single output source:

```xml
<CompositeFlow id="combiner_flow">
    <input-sources>
        <include source="AMEX" />
        <include source="NASDAQ" />
        <include source="NYSE" />
        <include source="OTCBB" />
    </input-sources>
    <output-sources>
        <include source="output_file" />
    </output-sources>
</CompositeFlow>
```

We could've also defined a single input with multiple outputs:

```xml
<CompositeFlow id="combiner_flow">
    <input-sources>
        <include source="AMEX" />
    </input-sources>
    <output-sources>
        <include source="output_csv" />
        <include source="output_fixed" />
        <include source="output_json" />
    </output-sources>
</CompositeFlow>
```

Broadway provides many options ingest, including file-monitoring capabilities. The following is an example of a file monitoring 
agent (FileTrigger) watching a path (e.g. "{{ user.home }}/broadway/incoming/tradingHistory") for four distinct file patterns 
via regular expressions (e.g. "```AMEX_(.*)[.]txt```", "```NASDAQ_(.*)[.]txt```", "```NYSE_(.*)[.]txt```" and "```OTCBB_(.*)[.]txt```").

```xml
<FileTrigger id="trading_history_trigger">
    <directory path="{{ user.home }}/broadway/incoming/tradingHistory" archive="DataStore">
        <feed pattern="AMEX_(.*)[.]txt">
            <SimpleFlow id="amex_file" input-source="input_file" output-source="output_file" />
        </feed>
        <feed pattern="NASDAQ_(.*)[.]txt">
            <SimpleFlow id="nasdaq_flow" input-source="input_file" output-source="output_file" />
        </feed>
        <feed pattern="NYSE_(.*)[.]txt">
            <SimpleFlow id="nyse_flow" input-source="input_file" output-source="output_file" />
        </feed>
        <feed pattern="OTCBB_(.*)[.]txt">
            <SimpleFlow id="otcbb_flow" input-source="input_file" output-source="output_file" />
        </feed>
    </directory>
</FileTrigger>
```

### Input Source Types

##### Text File Input

Currently Broadway offers a single input source type; however, more will be added soon, including Kafka, RDBMS and others.

```xml
<TextFileInputSource id="AMEX.txt" 
                    path="./app-cli/src/test/resources/files/AMEX.txt" 
                    layout="eod_company_input_layout" />
```


### Output Source Types

Broadway provides a number of options for data persistence, including Azure DocumentDB, Kafka, MongoDB, RDBMS, and Text Files (delimited, CSV, Fixed-length, JSON and soon XML).

##### Azure DocumentDB Output

```xml
<DocumentDBOutputSource id="docdb_output"
                        database="broadway"
                        collection="powerball_history"
                        host="{{ documentdb.secret.host }}"
                        master-key="{{ documentdb.secret.master_key }}"
                        consistency-level="Session"
                        layout="json_layout" />
```

##### Kafka Output

```xml
<KafkaOutputSource id="kafka-topic" 
                    topic="shocktrade.companies.avro" 
                    connectionString="vault114:2181" 
                    layout="avro_layout" />
```

##### MongoDB Output

```xml
<MongoOutputSource id="mongo1" 
                    servers="localhost" 
                    database="shocktrade" 
                    collection="test_companies" 
                    layout="mongo-output"/>
```

##### RDBMS/SQL Output

```xml
<SQLOutputSource id="sql_output"
                 table="dbo.tradingHistory"
                 layout="sql_layout"
                 driver="com.microsoft.sqlserver.jdbc.SQLServerDriver"
                 url="jdbc:sqlserver://ladaniel.database.windows.net:1433;database=ladaniel_sql"
                 user="{{ sqlserver.secret.user }}" password="{{ sqlserver.secret.password }}" />
```

##### Text File Output

```xml
<TextFileOutputSource id="output.txt" 
                        path="{{ java.io.tmpdir }}/eod_companies_fixed.txt" 
                        layout="fixed-output" />
```
