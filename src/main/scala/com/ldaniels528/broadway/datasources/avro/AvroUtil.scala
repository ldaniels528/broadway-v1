package com.ldaniels528.broadway.datasources.avro

import com.mongodb.casbah.Imports.{DBObject => O, _}
import org.apache.avro.generic.GenericRecord

/**
 * Avro Utility
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object AvroUtil {

  implicit class GenericRecordExtensions(val rec: GenericRecord) extends AnyVal {

    def asOpt[T](key: String): Option[T] = Option(rec.get(key)).map(_.asInstanceOf[T])

    def toMongoDB(keys: Seq[String]): O = {
      val values = keys.map(key => (key, rec.get(key)))
      values.foldLeft[O](O()) { case (doc, (key, value)) =>
        doc ++ (key -> value)
      }
    }

  }

}
