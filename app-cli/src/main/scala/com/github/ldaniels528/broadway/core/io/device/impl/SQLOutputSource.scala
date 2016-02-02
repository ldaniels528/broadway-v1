package com.github.ldaniels528.broadway.core.io.device.impl

import java.sql.{Connection, DriverManager, PreparedStatement}

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.io.device.impl.SQLOutputSource._
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.record.DataTypes._
import com.github.ldaniels528.broadway.core.io.record.Record
import com.ldaniels528.commons.helpers.OptionHelper.Risky._
import com.ldaniels528.commons.helpers.OptionHelper._
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap

/**
  * SQL Output Source
  * @author lawrence.daniels@gmail.com
  */
case class SQLOutputSource(id: String, connectionInfo: SQLConnectionInfo, layout: Layout) extends OutputSource {
  private val logger = LoggerFactory.getLogger(getClass)
  private var connection: Option[Connection] = None
  private val psCache = TrieMap[String, PreparedStatement]()
  private val sqlCache = TrieMap[String, String]()

  override def close(implicit scope: Scope) = {
    try connection.foreach(_.close()) finally connection = None
  }

  override def open(implicit scope: Scope) = {
    psCache.clear()
    sqlCache.clear()
    connection = connectionInfo.connect()
  }

  override def writeRecord(record: Record)(implicit scope: Scope) = {
    val sql = sqlCache.getOrElseUpdate(record.id, createInsertSQL(record))
    val ps = getPreparedStatement(sql)

    // populate the prepared statement
    record.fields zip record.fields.indices.map(_ + 1) foreach { case (field, index) =>
      field.value match {
        case Some(value) => ps.setObject(index, value)
        case None => ps.setNull(index, getSQLType(field.`type`))
      }
    }

    // perform the update
    updateCount(ps.executeUpdate())
  }

  private def createInsertSQL(record: Record) = {
    val columns = record.fields.map(_.name).mkString(", ")
    val values = record.fields.map(_ => "?").mkString(", ")
    val sql = s"INSERT INTO ${record.id} ($columns) VALUES ($values)"
    logger.info(s"SQL: $sql")
    sql
  }

  private def createUpdateSQL(record: Record) = {
    val pairs = record.fields.map(f => s"${f.name} = ?").mkString(", ")
    val sql = s"UPDATE ${record.id} SET $pairs WHERE ...." // TODO new to identify the condition fields
    logger.info(s"SQL: $sql")
    sql
  }

  private def getPreparedStatement(sql: String) = {
    psCache.getOrElseUpdate(sql, connection.map(_.prepareStatement(sql)) orDie "Connection not available. Use open()")
  }

  private def getSQLType(`type`: DataType) = {
    SQLTypeMapping.get(`type`) orDie s"Unhandled data type - ${`type`}"
  }

}

/**
  * SQL Output Source Companion Object
  * @author lawrence.daniels@gmail.com
  */
object SQLOutputSource {

  val SQLTypeMapping = Map(
    BINARY -> java.sql.Types.VARBINARY,
    BOOLEAN -> java.sql.Types.BOOLEAN,
    DOUBLE -> java.sql.Types.DOUBLE,
    FLOAT -> java.sql.Types.FLOAT,
    INT -> java.sql.Types.INTEGER,
    LONG -> java.sql.Types.BIGINT,
    STRING -> java.sql.Types.VARCHAR
  )

  /**
    * SQL Connection Information
    * @param driver the JDBC driver class (e.g. "com.microsoft.sqlserver.jdbc.SQLServerDriver")
    * @param url the JBDC URL (e.g. "jdbc:sqlserver://ladaniel.database.windows.net:1433;database=ladaniel_sql")
    * @param user the JDBC user name
    * @param password the JDBC user password
    */
  case class SQLConnectionInfo(driver: String, url: String, user: String, password: String) {

    def connect()(implicit scope: Scope) = {
      Class.forName(scope.evaluate(driver))
      DriverManager.getConnection(scope.evaluate(url), scope.evaluate(user), scope.evaluate(password))
    }
  }

}
