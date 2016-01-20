package com.github.ldaniels528.broadway.core.io.device.nosql

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.nosql.MongoDbOutputDevice._
import com.github.ldaniels528.broadway.core.io.device.{OutputDevice, StatisticsGeneration}
import com.github.ldaniels528.broadway.core.io.layout.OutputLayout
import com.github.ldaniels528.broadway.core.RuntimeContext
import com.ldaniels528.commons.helpers.OptionHelper.Risky._
import com.mongodb.ServerAddress
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsBoolean, _}

import scala.concurrent.{ExecutionContext, Future}

/**
  * MongoDB Output Device
  */
case class MongoDbOutputDevice(id: String, serverList: String, database: String, collection: String, layout: OutputLayout)
  extends OutputDevice with StatisticsGeneration {

  private var mongoConn: Option[MongoConnection] = None
  private var mongoDb: Option[MongoDB] = None
  private var mongoColl: Option[MongoCollection] = None
  private val writeConcern: WriteConcern = WriteConcern.JournalSafe
  private val logger = LoggerFactory.getLogger(getClass)

  override def open(rt: RuntimeContext) = {
    mongoConn match {
      case Some(device) =>
        logger.warn(s"Connection '$id' is already open")
      case None =>
        mongoConn = MongoConnection(makeServerList(serverList))
        mongoDb = mongoConn.map(_.apply(database))
        mongoColl = mongoDb.map(_.apply(collection))
    }
  }

  override def close(rt: RuntimeContext)(implicit ec: ExecutionContext) = {
    Future.successful(mongoConn.foreach(_.close()))
  }

  override def write(data: Data) = {
    (for {
      mc <- mongoColl
      jsString <- layout.encode(count, data)
    } yield {
      val js = Json.parse(jsString)
      val doc = toDocument(js.asInstanceOf[JsObject])
      val result = mc.insert(doc, writeConcern)
      updateCount(1 /*result.get.getN*/) // TODO why aren't the number of records updated returned?
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
  * MongoDB Output Device Companion Object
  */
object MongoDbOutputDevice {

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
