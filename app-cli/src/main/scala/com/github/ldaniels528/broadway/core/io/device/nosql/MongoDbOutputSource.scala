package com.github.ldaniels528.broadway.core.io.device.nosql

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.io.device.nosql.MongoDbOutputSource._
import com.github.ldaniels528.broadway.core.io.layout.json.MongoDbLayout
import com.github.ldaniels528.broadway.core.scope.Scope
import com.ldaniels528.commons.helpers.OptionHelper.Risky._
import com.mongodb.ServerAddress
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsBoolean, _}

/**
  * MongoDB Output Source
  */
case class MongoDbOutputSource(id: String,
                               serverList: String,
                               database: String,
                               collection: String,
                               layout: MongoDbLayout)
  extends OutputSource {

  private var mongoConn: Option[MongoConnection] = None
  private var mongoDb: Option[MongoDB] = None
  private var mongoColl: Option[MongoCollection] = None
  private val writeConcern: WriteConcern = WriteConcern.JournalSafe
  private val logger = LoggerFactory.getLogger(getClass)

  override def open(scope: Scope) = {
    scope ++= Seq(
      "flow.output.database" -> database,
      "flow.output.collection" -> collection,
      "flow.output.servers" -> serverList,
      "flow.output.writeConcern" -> writeConcern.toString
    )
    mongoConn match {
      case Some(device) =>
        logger.warn(s"Connection '$id' is already open")
      case None =>
        mongoConn = MongoConnection(makeServerList(serverList))
        mongoDb = mongoConn.map(_.apply(database))
        mongoColl = mongoDb.map(_.apply(collection))
    }
  }

  override def close(scope: Scope) {
    mongoConn.foreach(_.close())
    mongoConn = None
  }

  override def write(scope: Scope, data: Data) = {
    (for {
      mc <- mongoColl
    } yield {
      val js = data.asJson
      val doc = toDocument(js.asInstanceOf[JsObject])
      val result = mc.insert(doc, writeConcern)
      updateCount(scope, 1) // TODO why aren't the number of records updated returned?
    }) getOrElse 0
  }

  private def toDocument(js: JsObject) = {
    js.fieldSet.foldLeft(DBObject()) { case (dbo, (name, jv)) =>
      dbo.put(name, unwrap(jv))
      dbo
    }
  }

  private def unwrap(jv: JsValue): AnyRef = {
    jv match {
      case ja: JsArray => ja.value.map(unwrap)
      case jb: JsBoolean => jb.value: java.lang.Boolean
      case jn: JsNumber => jn.value
      case js: JsString => js.value
      case ju =>
        throw new IllegalStateException(s"Unable to unwrap '$ju' (${Option(ju).map(_.getClass.getName).orNull})")
    }
  }

}

/**
  * MongoDB Output Source Companion Object
  */
object MongoDbOutputSource {

  // register the time/date helpers
  RegisterJodaTimeConversionHelpers()

  /**
    * Creates a collection of server address instances from the given hosts string
    *
    * @param hosts given hosts string (e.g. "server1:27017,server2:27017,server3:27018")
    * @return a collection of [[ServerAddress server address]] instances
    */
  def makeServerList(hosts: String): List[ServerAddress] = {
    hosts.split("[,]").toList flatMap { pair =>
      pair.split("[:]").toList match {
        case host :: port :: Nil => Option(new ServerAddress(host, port.toInt))
        case host :: Nil => Option(new ServerAddress(host, 27017))
        case _ => None
      }
    }
  }

}
