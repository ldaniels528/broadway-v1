package com.ldaniels528.broadway.core.actors.nosql

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.nosql.MongoDBActor._
import com.mongodb.casbah.Imports.{DBObject => Q, _}
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.{DBObject, ServerAddress}

import scala.collection.concurrent.TrieMap

/**
 * The MongoDB Actor is capable of executing `find`, `findOne`, `insert`, `update`, and `upsert` commands against a
 * MongoDB server instance. NOTE: `find` and `findOne` queries require an actor a a recipient for retrieved records.
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class MongoDBActor(client: () => MongoClient, databaseName: String) extends Actor {
  private val collections = TrieMap[String, MongoCollection]()
  private var conn_? : Option[MongoClient] = None

  // register the time/date helpers
  RegisterJodaTimeConversionHelpers()

  override def preStart() = conn_? = Option(client())

  override def postStop() {
    conn_?.foreach(_.close())
    conn_? = None
  }

  override def receive = {
    case Find(name, query, fields) =>
      val theSender = sender()
      getCollection(name).foreach(_.find(query, fields) foreach (theSender ! MongoResult(_)))

    case FindAndModify(name, query, fields, sort, update, remove, returnNew, upsert) =>
      val theSender = sender()
      getCollection(name).foreach(_.findAndModify(query, fields, sort, remove, update, returnNew, upsert) foreach (theSender ! _))

    case FindAndRemove(name, query) =>
      getCollection(name).foreach(_.findAndRemove(query))

    case FindOne(name, query, fields) =>
      val theSender = sender()
      getCollection(name).foreach(theSender ! _.findOne(query, fields))

    case FindOneByID(name, id, fields) =>
      val theSender = sender()
      getCollection(name).foreach(theSender ! _.findOneByID(id, fields))

    case Insert(name, doc, concern) =>
      val theSender = sender()
      getCollection(name).foreach(theSender ! _.insert(doc))

    case Save(name, doc, concern) =>
      val theSender = sender()
      getCollection(name).foreach(theSender ! _.save(doc, concern))

    case Update(name, query, doc, multi, concern) =>
      val theSender = sender()
      getCollection(name).foreach(theSender ! _.update(query, doc, upsert = false, multi, concern))

    case Upsert(name, query, doc, multi, concern) =>
      val theSender = sender()
      getCollection(name).foreach(theSender ! _.update(query, doc, upsert = true, multi, concern))

    case message =>
      unhandled(message)
  }

  private def getCollection(name: String) = conn_?.map(c => collections.getOrElseUpdate(name, c(databaseName)(name)))

}

/**
 * MongoDB Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object MongoDBActor {

  /**
   * Creates a new database connection
   */
  def apply(connectionURL: String, databaseName: String) = {
    new MongoDBActor(() => MongoClient(new MongoClientURI(connectionURL)), databaseName)
  }

  /**
   * Creates a new database connection
   */
  def apply(addresses: List[ServerAddress], databaseName: String) = {
    new MongoDBActor(() => MongoClient(addresses), databaseName)
  }

  /**
   * Parses a MongoDB server list
   * @param serverList the given MongoDB server list (e.g. "dev601:27017,dev602:27017,dev603:27017")
   * @return a [[List]] of [[ServerAddress]] objects
   */
  def parseServerList(serverList: String): List[ServerAddress] = {
    serverList.split("[,]").toList map { server =>
      server.split("[:]").toList match {
        case host :: port :: Nil => new ServerAddress(host, port.toInt)
        case s =>
          throw new IllegalArgumentException(s"Illegal server address '$s', expected format 'host:port'")
      }
    }
  }

  case class Find(name: String, query: DBObject, fields: DBObject = Q())

  case class FindOne(name: String, query: DBObject, fields: DBObject = Q())

  case class FindOneByID(name: String, id: String, fields: DBObject = Q())

  case class FindAndModify(name: String,
                           query: DBObject,
                           fields: DBObject = Q(),
                           sort: DBObject = Q(),
                           update: DBObject,
                           remove: Boolean = false,
                           returnNew: Boolean = true,
                           upsert: Boolean = false)

  case class FindAndRemove(name: String, query: DBObject)

  case class Insert(name: String, doc: DBObject, concern: WriteConcern = WriteConcern.JournalSafe)

  case class Save(name: String, doc: DBObject, concern: WriteConcern = WriteConcern.JournalSafe)

  case class Update(name: String,
                    query: DBObject,
                    doc: DBObject,
                    multi: Boolean = false,
                    concern: WriteConcern = WriteConcern.JournalSafe)

  case class Upsert(name: String,
                    query: DBObject,
                    doc: DBObject,
                    multi: Boolean = false,
                    concern: WriteConcern = WriteConcern.JournalSafe)

  case class MongoResult(doc: DBObject)

}
