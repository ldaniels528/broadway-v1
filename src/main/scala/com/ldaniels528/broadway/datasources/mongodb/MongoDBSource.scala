package com.ldaniels528.broadway.datasources.mongodb

import com.mongodb.MongoClientOptions
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers

/**
 * Represents a non-blocking MongoDB data source
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class MongoDBSource(database: String,
                    replicas: List[ServerAddress],
                    defaultConcern: WriteConcern = WriteConcern.Safe) {

  // register the time/date helpers
  RegisterJodaTimeConversionHelpers()

  /**
   * Returns a new reference to the specified collection
   */
  def getCollection(name: String)(implicit conn: MongoConnection) = conn(database)(name)

  /**
   * Creates a new database connection
   */
  def getConnection = {
    // create the options
    val options = MongoClientOptions
      .builder()
      .connectionsPerHost(200)
      .maxWaitTime(100)
      .socketKeepAlive(true)
      .threadsAllowedToBlockForConnectionMultiplier(50)
      .build()

    // create the connection
    MongoConnection(replicas, new MongoOptions(options))
  }


}
