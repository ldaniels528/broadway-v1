package com.ldaniels528.broadway.datasources.cassandra

import com.datastax.driver.core._
import com.google.common.util.concurrent.{FutureCallback, Futures}

import scala.concurrent.{Future, Promise}

/**
 * Represents a non-blocking Cassandra data source
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class CassandraSource(cluster: Cluster) {

  /**
   * Asynchronously execute the given CQL query
   * @param query the given CQL query
   * @param session the session instance
   * @return a promise of a result set
   */
  def execute(query: String, args: Any*)(implicit session: Session) = invokeCQL(query, toObjects(args): _*)

  /**
   * Asynchronously inserts the given key value pairs into the given table
   * @param table the given table name
   * @param args the given key value pairs
   * @param session the given [[Session]]
   * @return a promise of a result set
   */
  def insert(table: String, args: (String, Any)*)(implicit session: Session): Future[ResultSet] = {
    val fields = args map (_._1)
    val query = s"INSERT INTO $table (${fields mkString ","}) VALUES (${fields map (_ => "?") mkString ","})"
    invokeCQL(query, toObjects(args map (_._2)))
  }

  /**
   * Asynchronously inserts the given key value pairs into the given table
   * @param table the given table name
   * @param bean the given bean, which contains the properties to insert
   * @param session the given [[Session]]
   * @return a promise of a result set
   */
  def insert[T](table: String, bean: T)(implicit session: Session): Future[ResultSet] = {
    insert(table, Cascade.mapify(bean).toSeq: _*)
  }

  def update[T](table: String, bean: T)(implicit session: Session): Future[ResultSet] = {
    val args = Cascade.mapify(bean).toSeq
    // TODO need key fields
    val fields = args map (_._1)
    val fieldExpr = fields map (f => s"$f = ?") mkString ","
    val whereExpr = fields map (f => "$f = ?") mkString " AND "
    invokeCQL(s"UPDATE $table SET $fieldExpr WHERE $whereExpr", toObjects(args map (_._2)))
  }

  /**
   * Opens a new session for the given key space
   * @param keySpace the given key space
   * @return a new session
   */
  def openSession(keySpace: String): Session = cluster.connect(keySpace)

  private def toObjects(values: Seq[Any]) = values map {
    case o: Option[_] => o.map(_.asInstanceOf[Object]).orNull
    case x => x.asInstanceOf[Object]
  }

  private def invokeCQL(query: String, args: Any*)(implicit session: Session) = makePromise(session.executeAsync(query, toObjects(args): _*))

  private def makePromise(resultSetFuture: ResultSetFuture): Future[ResultSet] = {
    val promise = Promise[ResultSet]()
    Futures.addCallback(resultSetFuture, new FutureCallback[ResultSet] {
      override def onSuccess(rs: ResultSet) = promise.success(rs)

      override def onFailure(cause: Throwable) = promise.failure(cause)
    })
    promise.future
  }

}

/**
 * Cassandra Source Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object CassandraSource {

  def apply(hosts: String*): CassandraSource = {
    val cluster = hosts.foldLeft(new Cluster.Builder()) { (builder, host) => builder.addContactPoint(host)}.build()
    new CassandraSource(cluster)
  }

}