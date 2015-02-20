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
    "BLOB" -> "Array[Byte]",
    "DATETIME" -> "Date",
    "DOUBLE" -> "Double",
    "INT" -> "Int",
    "LONGTEXT" -> "String",
    "TEXT" -> "String",
    "TIMESTAMP" -> "Date",
    "VARBINARY" -> "Array[Byte]",
    "VARCHAR" -> "String")

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
   * Generates the Slick database classes
   * @param catalog the given database catalog
   * @param classes the given [[ClassInfo]] instances
   * @param outputDirectory the given output directory
   * @see http://stackoverflow.com/questions/22626328/hello-world-example-for-slick-2-0-with-mysql
   */
  def generateClasses(catalog: String, classes: Seq[ClassInfo], outputDirectory: File): Unit = {
    classes foreach { classInfo =>
      import classInfo.{className, fields, tableName}

      // create the package directory
      val packageName = catalog.toLowerCase
      val packageDirectory = new File(outputDirectory, packageName)
      if (!packageDirectory.exists()) packageDirectory.mkdirs()

      // create a source file
      val classFile = new File(packageDirectory, s"$className.scala")
      logger.info(s"Generating '${classFile.getAbsolutePath}'...")

      // generate the import statements
      var imports = List("scala.slick.driver.MySQLDriver.simple._")
      if (classInfo.fields.exists(_.typeName == "Date")) imports = "java.sql.Date" :: imports

      // generate the Slick column functions
      val functions = {
        fields.map(f => s"""def ${f.fieldName} = column[${f.typeName}]("${f.columnName}")""") ++
          Seq(s"def * = (${fields.map(_.fieldName).mkString(", ")})")
      }.indent(tabs = 2)

      // generate the source code
      new BufferedOutputStream(new FileOutputStream(classFile), 8192) use { out =>
      out.write(
          s"""|package $packageName
              |
              |${imports map (i => s"import $i\n") mkString}
              |case class $className(${fields.map(f => s"${f.fieldName}: ${f.typeName}").mkString(", ")})
              |
              |object $className {
              |
              |  class ${className.toPlural}(tag: Tag) extends Table[(${fields.map(_.typeName).mkString(", ")})](tag, "$tableName") {
              |$functions
              |  }
              |}
              |""".stripMargin('|').trim.getBytes("UTF-8"))
      }
    }
    logger.info(s"${classes.size} source file(s) generated.")
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
      val tableTypes = (metadata.getTableTypes.toMap flatMap (_ map (_._2.asInstanceOf[String]) toSeq)).toArray

      // lookup all tables within the catalog
      val tables = metadata.getTables(catalog, null, null, tableTypes).toMap flatMap (_.get("TABLE_NAME") map (_.asInstanceOf[String]))

      // transform the table mappings into class information
      tables map { tableName =>
        val className = tableName.toCamelCase
        val columns = metadata.getColumns(catalog, null, tableName, null).toMap
        val fields = columns flatMap { column =>
          for {
            columnName <- column.get("COLUMN_NAME") map (_.asInstanceOf[String])
            typeName <- column.get("TYPE_NAME") map (_.asInstanceOf[String])
            columnSize <- column.get("COLUMN_SIZE") map (_.asInstanceOf[Int])
            ordinalPosition <- column.get("ORDINAL_POSITION") map (_.asInstanceOf[Int])
            nullable <- column.get("IS_NULLABLE") map (_ == "YES")
          //_ = logger.info(s"columns: ${column.toSeq.filterNot{ case (k,v) => v == null }}")
          } yield FieldInfo(columnName, columnName.toSnakeCase, typeName.toScalaType(nullable), nullable, columnSize, ordinalPosition)
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
  def getConnection(url: String, configPath: String): Connection = {
    val props = loadConnectionProperties(configPath)
    val conn = java.sql.DriverManager.getConnection(url, props)
    if (conn == null) throw new IllegalStateException(s"Unable to establish connection to $url")
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
    logger.debug(s"Loaded properties: $p")
    p
  }

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
   * @param nullable indicates whether the column value is nullable
   * @param columnSize the defined column size
   * @param ordinalPosition the original position of the column within the table
   */
  case class FieldInfo(columnName: String, fieldName: String, typeName: String, nullable: Boolean, columnSize: Int, ordinalPosition: Int)

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

  /**
   * String Conversion Utility Functions
   * @param noun the given host string
   */
  implicit class StringConversions(val noun: String) extends AnyVal {

    /**
     * Returns the plural form of the given noun (e.g. "activity" returns "activities")
     * @return the plural noun
     */
    def toPlural: String = {
      noun match {
        case s if s.endsWith("y") => s.dropRight(1) + "ies"
        case s if s.endsWith("s") | s.endsWith("x") => s + "es"
        case s => s + "s"
      }
    }

    /**
     * Converts the given named identifier to camel case (e.g. "TheBigRedBall")
     * @return the named identifier as camel case
     */
    def toCamelCase: String = {
      noun match {
        case s if s.contains("_") => s.split("[_]") map (_.toLowerCase) map (s => s.head.toUpper + s.tail) mkString
        case s if s.forall(_.isUpper) => s.head.toUpper + s.tail.toLowerCase
        case s => s.head.toUpper + s.tail
      }
    }

    /**
     * Converts the given named identifier to snake case (e.g. "theBigRedBall")
     * @return the named identifier as snake case
     */
    def toSnakeCase: String = {
      noun match {
        case s if s.contains("_") =>
          val items = s.split("[_]") map (_.toLowerCase)
          (items.head ++ items.tail.map(s => s.head.toUpper + s.tail)) mkString
        case s if s.forall(_.isUpper) => s.toLowerCase
        case s => s.head.toLower + s.tail
      }
    }

  }

  /**
   * String Format Conversion Utility Functions
   * @param lines the given lines of the paragraph to format
   */
  implicit class StringFormatConversions(val lines: Seq[String]) extends AnyVal {

    /**
     * Indents the given lines with a tab and ends each line with a carriage return
     * @return a string representing the paragraph of lines
     */
    def indent(tabs: Int): String = lines map ("\t" * tabs + _) mkString "\n"

  }

  /**
   * SQL to Scala Type Conversions
   * @param typeName the given type name
   */
  implicit class TypeConversion(val typeName: String) extends AnyVal {

    /**
     * Returns the given SQL type name as the equivalent Scala type (e.g. "BIGINT" return "Long")
     * @return the equivalent Scala type (e.g. "Long")
     */
    def toScalaType(nullable: Boolean): String = {
      val myTypeName = TypeNameMapping.getOrElse(typeName, typeName)
      if(nullable) s"Option[$myTypeName]" else myTypeName
    }

  }

}