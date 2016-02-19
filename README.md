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

* [Java SDK 1.8] (http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Scala 2.11.7] (http://scala-lang.org/download/)
* [SBT 0.13.9] (http://www.scala-sbt.org/download.html)

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

### Import Directive

Broadway provides the capability to importing additional configuration entities. This is accomplished via the `import` directive.
```xml
<import path="./app-cli/src/test/resources/global_settings.xml" />
```

These imported files may contain any valid directive, and could you be used to avoid defining frequently referenced input/output sources,
layouts, archival strategies, etc.

In the earlier story example, the following was defined in the file `global_settings.xml`:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<story id="global_settings">

    <properties file="{{ user.home }}/connection.properties" />

    <archives>
        <FileArchive id="DataStore" base="{{ user.home }}/broadway/archive" compression="gzip" />
    </archives>

    <layouts>
        <MultiPartLayout id="eod_company_input_layout">
            <header>
                <record id="delimited_header" format="delimited" delimiter="\t">
                    <field name="symbol" type="string"/>
                    <field name="description" type="string"/>
                </record>
            </header>
            <body>
                <record id="delimited_data" format="delimited" delimiter="\t">
                    <field name="symbol" type="string"/>
                    <field name="description" type="string"/>
                </record>
            </body>
        </MultiPartLayout>

        <MultiPartLayout id="eod_history_input_layout">
            <header>
                <record id="input_header" format="csv">
                    <field name="ticker" type="string">&lt;ticker&gt;</field>
                    <field name="date" type="string">&lt;date&gt;</field>
                    <field name="open" type="string">&lt;open&gt;</field>
                    <field name="high" type="string">&lt;high&gt;</field>
                    <field name="low" type="string">&lt;low&gt;</field>
                    <field name="close" type="string">&lt;close&gt;</field>
                    <field name="volume" type="string">&lt;vol&gt;</field>
                </record>
            </header>
            <body>
                <record id="input_body" format="csv">
                    <field name="ticker" type="string" />
                    <field name="date" type="string" />
                    <field name="open" type="string" />
                    <field name="high" type="string" />
                    <field name="low" type="string" />
                    <field name="close" type="string" />
                    <field name="volume" type="string" />
                </record>
            </body>
        </MultiPartLayout>
    </layouts>
</story>
```

### Properties Directive

Broadway provides the capability of loading predefined configuration properties from a file. These properties could be details that
you want to hide for security reasons, or frequently used properties that you don't want to define in every story you create.

```xml
<properties file="{{ user.home }}/connection.properties" />
```

### Flow Control

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
        <include source="output_csv" />
    </output-sources>
</CompositeFlow>
```

We could've also defined a single input with multiple outputs:

```xml
<CompositeFlow id="combiner_flow">
    <input-sources>
        <include source="NASDAQ" />
    </input-sources>
    <output-sources>
        <include source="output_csv" />
        <include source="output_fixed" />
        <include source="output_json" />
    </output-sources>
</CompositeFlow>
```

Or, using the simplest flow control option... A single input and a single output source.

```xml
<SimpleFlow id="nyse_flow" input-source="NASDAQ" output-source="output_csv" />
```

### Triggers

Broadway also provides many data ingestion options, including file-monitoring capabilities. The following is an example of a file monitoring 
agent (FileTrigger) watching a path (e.g. "{{ user.home }}/broadway/incoming/tradingHistory") for four distinct file patterns 
via regular expressions (e.g. "```AMEX_(.*)[.]txt```", "```NASDAQ_(.*)[.]txt```", "```NYSE_(.*)[.]txt```" and "```OTCBB_(.*)[.]txt```").
Once a file is detected, a flow is kicked off, in this case, each file feed contains a ```SimpleFlow``` directive, indicating how to process the file.

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

### File Archival Strategies

Broadway provides a mechanism for archiving files. This is normally used in conjunction with a ```FileTrigger``` directive. Simply put
 the archive stores (and optionally compresses) files after they have been processed.

```xml
<archives>
    <FileArchive id="DataStore" base="{{ user.home }}/broadway/archive" compression="gzip" />
</archives>
```

### Layouts

Broadway uses the concept of a ```layout``` to define the input or output format of a data source. Data formats like CSV, JSON, Avro and others
are all made possible via layout definitions.

##### JSON Formatting

The following example is a simple JSON layout, with two fields, ```symbol``` and ```description```.
```xml
<MultiPartLayout id="json_layout">
    <body>
        <record id="json_body" format="json">
            <field name="symbol" type="string" value="{{ symbol }}" />
            <field name="description" type="string" value="{{ description }}" />
        </record>
    </body>
</MultiPartLayout>
```

The layout above describes data that looks like the following:
```json
{"symbol":"AADR","description":"BNY Mellon Focused Growth ADR ETF"}
```

##### Avro Formatting

Additionally, the Avro format (a binary JSON derivative) is also supported:
```xml
<MultiPartLayout id="avro_layout">
    <body>
        <record id="avro_body" format="avro" name="EodCompanyInfo" namespace="com.shocktrade.avro" doc="EOD Data companies schema">
            <field name="symbol" type="string">{{ symbol }}</field>
            <field name="description" type="string">{{ description }}</field>
        </record>
    </body>
</MultiPartLayout>
```

##### Text Formatting

You can, however, define much more complex layouts with optional header and trailer records. Consider the following:
```xml
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
```

The layout above describes data that could looks like the following:
```
Symbol    Description                                       Source                                  Line Number 
AA.P      Alcoa Inc Pf 3.75                                 AMEX.txt                                2           
AADR      BNY Mellon Focused Growth ADR ETF                 AMEX.txt                                3                  
```

### Input Source Types

Currently Broadway offers a single input source type; however, more will be added soon, including Kafka, RDBMS and others.

##### Text File Input

```xml
<TextFileInputSource id="AMEX" 
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
                    connectionString="localhost:2181" 
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
                 url="jdbc:sqlserver://testing.database.windows.net:1433;database=ladaniel_sql"
                 user="{{ sqlserver.secret.user }}" password="{{ sqlserver.secret.password }}" />
```

##### Text File Output

```xml
<TextFileOutputSource id="output.txt" 
                        path="{{ java.io.tmpdir }}/eod_companies_fixed.txt" 
                        layout="fixed-output" />
```
