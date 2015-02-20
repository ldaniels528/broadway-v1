package com.ldaniels528.broadway.core.jdbc

import java.io._
import java.sql.{Connection, ResultSet}

import com.ldaniels528.trifecta.util.ResourceHelper._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.language.postfixOps

/**
 * MySQL Catalog to Slick Class Generator
 * @author lawrence.daniels@gmail.com
 */
object MySQLtoSlickGenerator {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val TypeNameMapping = Map(
    "BIGINT" -> "Long",
    "BIT" -> "Boolean",
    "DATETIME" -> "Date",
    "DOUBLE" -> "Double",
    "INT" -> "Int",
    "LONGTEXT" -> "String",
    "TEXT" -> "String",
    "TIMESTAMP" -> "Date",
    "VARBINARY" -> "Array[Byte]",
    "VARCHAR" -> "String"
  )

  /**
   * Main entry point
   * @param args the given command line arguments
   */
  def main(args: Array[String]): Unit = {
    args.toList match {
      case url :: catalog :: configPath :: outputPath :: Nil =>
        val classes = extractClassInfo(catalog, getConnection(url, configPath))
        generateClasses(catalog, classes, new File(outputPath))
      case _ =>
        throw new IllegalArgumentException(s"${getClass.getName} <url> <catalog> <configFilePath> <outputPath>")
    }
  }

  /**
   * Generates entity classes foreach table within the given catalog (database)
   * @param catalog the given catalog (database)
   * @param conn the given [[Connection]]
   */
  def extractClassInfo(catalog: String, conn: Connection): Seq[ClassInfo] = {
    try {
      // get the database metadata
      val metadata = conn.getMetaData

      // display the database product name and version
      val (productName, productVersion) = (metadata.getDatabaseProductName, metadata.getDatabaseProductVersion)
      logger.info(s"Connected to $productName v$productVersion")

      // lookup the defined table types
      val tableTypes = (metadata.getTableTypes.toMap flatMap (_ map (_._2.toString) toSeq)).toArray

      // lookup all tables within the catalog
      val tables = metadata.getTables(catalog, null, null, tableTypes).toMap flatMap (_.get("TABLE_NAME") map (_.asInstanceOf[String]))

      // transform the table mappings into class information
      tables map { tableName =>
        val className = toCamelCase(tableName)
        val columns = metadata.getColumns(catalog, null, tableName, null).toMap
        val fields = columns flatMap { column =>
          for {
            columnName <- column.get("COLUMN_NAME") map (_.asInstanceOf[String])
            typeName <- column.get("TYPE_NAME") map (_.asInstanceOf[String])
            columnSize <- column.get("COLUMN_SIZE") map (_.asInstanceOf[Int])
            sqlDataType <- column.get("SQL_DATA_TYPE") map (_.asInstanceOf[Int])
            ordinalPosition <- column.get("ORDINAL_POSITION") map (_.asInstanceOf[Int])
          } yield FieldInfo(columnName, toSnakeCase(columnName), toScalaType(typeName), sqlDataType, columnSize, ordinalPosition)
        }

        // create a class info instance with sorted fields
        ClassInfo(tableName, className, fields.sortBy(_.ordinalPosition))
      }
    }
    finally {
      conn.close()
    }
  }

  /**
   * Creates a database connection
   * @param configPath the given configuration file path
   * @return a database [[Connection]]
   */
  private def getConnection(url: String, configPath: String): Connection = {
    val props = loadConnectionProperties(configPath)
    val conn = java.sql.DriverManager.getConnection(url, props)
    if(conn == null) throw new IllegalStateException(s"Unable to establish connection to $url")
    conn
  }

  /**
   * Loads the connection properties
   * @param resourcePath the given resource path
   * @return the connection properties
   */
  private def loadConnectionProperties(resourcePath: String): java.util.Properties = {
    val p = new java.util.Properties()
    val localFile = new File(resourcePath)
    logger.info(s"Attempting to load properties file '${localFile.getAbsolutePath}'...")

    if (localFile.exists()) new FileInputStream(resourcePath) use p.load
    else throw new FileNotFoundException(s"File '$resourcePath' not found")
    logger.info(s"properties = $p")
    p
  }

  /**
   * Generates the Slick database classes
   * @param catalog the given database catalog
   * @param classes the given [[ClassInfo]] instances
   * @param outputDirectory the given output directory
   * @see http://stackoverflow.com/questions/22626328/hello-world-example-for-slick-2-0-with-mysql
   */
  private def generateClasses(catalog: String, classes: Seq[ClassInfo], outputDirectory: File): Unit = {
    classes foreach { classInfo =>
      import classInfo.{className, fields, tableName}

      val classFile = new File(outputDirectory, s"$className.scala")
      logger.info(s"Generating class '${classFile.getAbsolutePath}'...")

      val functions = fields.map(f => "\t" + s"""def ${f.fieldName} = column[${f.typeName}]("${f.columnName}")""").mkString("\n")
      val projection = s"\tdef * = (" + fields.map(_.fieldName).mkString(", ") + ")"
      val classData =
        s"""|package ${catalog.toLowerCase}
            |
            |import java.sql.Date
            |import scala.slick.driver.MySQLDriver.simple._
            |
            |case class $className(${fields.map(f => s"${f.fieldName}: ${f.typeName}").mkString(", ")})
            |
            |class ${toPlural(className)}(tag: Tag) extends Table[(${fields.map(_.typeName).mkString(", ")})](tag, "$tableName") {
            |$functions
            |$projection
            |}
            |""".stripMargin('|').trim

      val out = new BufferedOutputStream(new FileOutputStream(classFile), 8192)
      out.write(classData.getBytes("UTF-8"))
      out.flush()
      out.close()
    }
    logger.info("Done.")
  }

  /**
   * Returns the plural form of the given noun (e.g. "activity" returns "activities")
   * @param noun the given singular noun
   * @return the plural noun
   */
  private def toPlural(noun: String): String = {
    noun match {
      case s if s.endsWith("y") => s.dropRight(1) + "ies"
      case s if s.endsWith("s") => s + "es"
      case s => s + "s"
    }
  }

  /**
   * Converts the given named identifier to camel case (e.g. "TheBigRedBall")
   * @param name the given named identifier
   * @return the named identifier as camel case
   */
  private def toCamelCase(name: String): String = {
    name match {
      case s if s.contains("_") => s.split("[_]") map (s => s.head.toUpper + s.tail) mkString
      case s => s.head.toUpper + s.tail
    }
  }

  /**
   * Converts the given named identifier to snake case (e.g. "theBigRedBall")
   * @param name the given named identifier
   * @return the named identifier as snake case
   */
  private def toSnakeCase(name: String): String = {
    name match {
      case s if s.contains("_") =>
        val items = s.split("[_]")
        (items.head ++ items.tail.map(s => s.head.toUpper + s.tail)) mkString
      case s => s.head.toLower + s.tail
    }
  }

  /**
   * Returns the given SQL type name as the equivalent Scala type (e.g. "BIGINT" return "Long")
   * @param typeName the given type name (e.g. "BIGINT")
   * @return the equivalent Scala type (e.g. "Long")
   */
  private def toScalaType(typeName: String): String = TypeNameMapping.getOrElse(typeName, typeName)

  /**
   * Represents Scala-Slick class information
   * @param tableName the name of the table being represented
   * @param className the name of the class being represented
   * @param fields the database columns / class member variables
   */
  case class ClassInfo(tableName: String, className: String, fields: Seq[FieldInfo])

  /**
   * Represents a Scala-Slick table column / member variable
   * @param columnName the name of the column
   * @param fieldName the name of the member variable
   * @param typeName the Scala type name (e.g. "Int")
   * @param sqlTypeID the given SQL type identifier
   * @param columnSize the defined column size
   * @param ordinalPosition the original position of the column within the table
   */
  case class FieldInfo(columnName: String, fieldName: String, typeName: String, sqlTypeID: Int, columnSize: Int, ordinalPosition: Int)

  /**
   * ResultSet Conversions
   * @param rs the given [[ResultSet]]
   */
  implicit class ResultSetConversions(val rs: ResultSet) extends AnyVal {

    /**
     * Transforms the results into a mapping of key-value pairs
     * @return a mapping of key-value pairs
     */
    def toMap: Seq[Map[String, AnyRef]] = {
      val columnNames = getColumnNames(rs)
      val buf = mutable.Buffer[Map[String, AnyRef]]()
      while (rs.next()) {
        buf += Map(columnNames map { label => (label, rs.getObject(label))}: _*)
      }
      buf
    }

    /**
     * Extracts the column names from the result set metadata
     * @param rs the given [[ResultSet]]
     * @return the collection of column names
     */
    private def getColumnNames(rs: ResultSet): Seq[String] = {
      val metadata = rs.getMetaData
      for (n <- 1 to metadata.getColumnCount) yield metadata.getColumnName(n)
    }

  }

}