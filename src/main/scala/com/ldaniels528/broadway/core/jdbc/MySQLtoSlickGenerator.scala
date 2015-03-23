package com.ldaniels528.broadway.core.jdbc

import java.io._
import java.sql.{Connection, ResultSet}

import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.util.ResourceHelper._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.language.postfixOps

/**
 * MySQL Catalog to Slick Model Generator
 * @author lawrence.daniels@gmail.com
 */
object MySQLtoSlickGenerator {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val DataTypeMapping = {
    import java.sql.Types._
    Map(
      BIGINT -> "Long",
      BIT -> "Boolean",
      BLOB -> "Array[Byte]",
      BOOLEAN -> "Boolean",
      CHAR -> "String",
      CLOB -> "Array[Char]",
      DATE -> "java.sql.Date",
      DECIMAL -> "Double", //BigDecimal?
      DOUBLE -> "Double",
      FLOAT -> "Float",
      REAL -> "Float",
      INTEGER -> "Int",
      JAVA_OBJECT -> "AnyRef",
      LONGNVARCHAR -> "String",
      LONGVARCHAR -> "String",
      LONGVARBINARY -> "Array[Byte]",
      NCHAR -> "String",
      OTHER -> "AnyRef",
      REAL -> "Float",
      SMALLINT -> "Short",
      SQLXML -> "java.sql.SQLXML",
      TIME -> "java.util.Date",
      TIMESTAMP -> "java.sql.Timestamp",
      TINYINT -> "Byte",
      VARBINARY -> "Array[Byte]",
      VARCHAR -> "String"
    )
  }

  /**
   * UPDATE_RULE (int) => What happens to foreign key when primary is updated:
   * importedKeyNoAction - do not allow update of primary key if it has been imported
   * importedKeyCascade - change imported key to agree with primary key update
   * importedKeySetNull - change imported key to NULL if its primary key has been updated
   * importedKeySetDefault - change imported key to default values if its primary key has been updated
   * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)
   */
  private val ImportedKeyUpdateRules = {
    import java.sql.DatabaseMetaData._
    Map(
      importedKeyNoAction -> "ForeignKeyAction.NoAction",
      importedKeyCascade -> "ForeignKeyAction.Cascade",
      importedKeyRestrict -> "ForeignKeyAction.Restrict",
      importedKeySetDefault -> "ForeignKeyAction.SetDefault",
      importedKeySetNull -> "ForeignKeyAction.SetNull"
    )
  }

  /**
   * Main entry point
   * @param args the given command line arguments
   */
  def main(args: Array[String]): Unit = {
    args.toList match {
      case configPath :: outputPath :: Nil => exportAsModels(configPath, outputPath)
      case _ =>
        throw new IllegalArgumentException(s"${getClass.getName} <configPath> <outputPath>")
    }
  }

  /**
   * Exports all tables within a catalog via JDBC as TypeSafe Slick model source files to the given output path
   * @param configPath the given JDBC configuration properties
   * @param outputPath the given output path
   */
  def exportAsModels(configPath: String, outputPath: String) = generateSources(extractTableModels(configPath), new File(outputPath))

  /**
   * Generates the Slick model source files
   * @param models the given collection of table models
   * @param outputDirectory the given output directory
   */
  private[jdbc] def generateSources(models: Seq[TableModel], outputDirectory: File): Unit = {
    var count = 0
    models foreach { model =>
      // fail-safe
      if (model.columnModels.length > 22)
        logger.warn(s"${model.tableName}: The maximum number of columns has been exceeded (${model.columnModels.length} > 22)")
      else {
        // create the package directory
        val packageDirectory = new File(outputDirectory, model.packageName)
        if (!packageDirectory.exists()) packageDirectory.mkdirs()

        // get a reference to the source code output file
        val sourceFile = new File(packageDirectory, s"${model.className}.scala")

        // generate the source code and write it to disk
        logger.info(s"Generating '${sourceFile.getAbsolutePath}'...")
        val sourceCode = generateSource(model)
        new BufferedOutputStream(new FileOutputStream(sourceFile), 8192) use (_.write(sourceCode.getBytes("UTF-8")))
        count += 1
      }
    }
    logger.info(s"$count source file(s) generated.")
  }

  /**
   * Generates a Slick model source file
   * @param model the given model class instance
   * @see http://stackoverflow.com/questions/22626328/hello-world-example-for-slick-2-0-with-mysql
   */
  private[jdbc] def generateSource(model: TableModel): String = {
    import model.{className, columnModels, packageName, tableName}

    val tableClassName = className.toPlural
    s"""|package $packageName
        |
        |${generateImports(columnModels) map ("import " + _) mkString "\n"}
        |
        |class $className(${columnModels.map(f => s"${f.fieldName}: ${f.typeName}").mkString(", ")})
                                                                                                     |
                                                                                                     |object $className {
                                                                                                                         |
                                                                                                                         | class $tableClassName(tag: Tag) extends Table[(${columnModels.map(_.typeName).mkString(", ")})](tag, "$tableName") {
                                                                                                                                                                                                                                             |${generateColumnFunctions(columnModels) indent (tabs = 2)}
        |${generateForeignKeys(model.foreignKeys) indent (tabs = 2) paragraph}
        | }
        |
        | val ${tableName.toSmallCamel.toPlural} = TableQuery[$tableClassName]
                                                                               |
                                                                               |}
                                                                               |""".stripMargin('|').trim
  }

  /**
   * Generates the column functions from the given collection of column models
   * @param columnModels the given collection of column models
   * @return the column functions (e.g. "def id = column[Long]("user_id", O.AutoInc, O.PrimaryKey)")
   */
  private[jdbc] def generateColumnFunctions(columnModels: Seq[ColumnModel]): List[String] = {
    val functions = columnModels.toList.map { c =>
      // create the column function arguments (name, options*)
      val args = (s""""${c.columnName}"""" ::
        (if (c.autoincrement) List("O.AutoInc") else Nil) :::
        (if (c.primaryKey.isDefined) List("O.PrimaryKey") else Nil)).mkString(", ")

      // define the column function
      s"def ${c.fieldName} = column[${c.typeName}]($args)"
    }
    functions ::: s"def * = (${columnModels.map(_.fieldName).mkString(", ")})" :: Nil
  }

  /**
   * Generates the foreign key functions from the collection of foreign key definitions
   * @param foreignKeys the collection of foreign key definitions
   * @return the foreign key functions
   */
  private[jdbc] def generateForeignKeys(foreignKeys: Seq[ForeignKey]): List[String] = {
    (foreignKeys.sortBy(_.keySeq) map { fk =>
      import fk._
      val pkTableSmallCamel = pkTableName.toSmallCamel
      val fkFuncName = s"${pkTableSmallCamel}By${fkColumnName.toBigCamel}"
      val fkObjName = s"${pkTableName.toBigCamel}.${pkTableSmallCamel.toPlural}"

      // define the function arguments
      val args = Seq(
        Some(s"_.${pkColumnName.toSmallCamel}"),
        ImportedKeyUpdateRules.get(fk.deleteRule) map ("onDelete = " + _),
        ImportedKeyUpdateRules.get(fk.updateRule) map ("onUpdate = " + _)).flatten.mkString(", ")

      // return the foreign key function
      s"""def $fkFuncName = foreignKey("$fkName", ${fkColumnName.toSmallCamel}, $fkObjName)($args)"""
    }).toList
  }

  /**
   * Generates the necessary import statements for compiling the generate source code
   * @param columnModels the given collection of column models
   * @return the import statements (e.g. "import java.sql.Date")
   */
  private[jdbc] def generateImports(columnModels: Seq[ColumnModel]): List[String] = {
    List("scala.slick.driver.MySQLDriver.simple._") :::
      (if (columnModels.exists(_.typeName.contains("Date"))) List("java.sql.Date") else Nil)
  }

  /**
   * Generates models for each table within the given catalog (database)
   * @param configPath the given configuration file path
   * @return a collection of table models
   */
  private[jdbc] def extractTableModels(configPath: String): Seq[TableModel] = {
    // load the configuration properties
    val props = loadConnectionProperties(configPath)
    val catalog = Option(props.getProperty("catalog")).orDie("Required property 'catalog' is missing")
    openConnection(props) use { conn =>
      // get the database metadata
      val metadata = conn.getMetaData

      // display the database product name and version
      val (productName, productVersion) = (metadata.getDatabaseProductName, metadata.getDatabaseProductVersion)
      logger.info(s"Connected to $productName v$productVersion")

      // lookup the defined table types
      val tableTypes = metadata.getTableTypes.transform(_.getString("TABLE_TYPE")).toArray

      // lookup all tables within the catalog
      val tables = metadata.getTables(catalog, null, null, tableTypes).transform(_.getString("TABLE_NAME"))

      // create a mapping of the foreign keys for each table
      logger.info(s"Gathering foreign key constraints for ${tables.length} tables...")
      val foreignKeys = Map(tables flatMap { tableName =>
        logger.info(s"Retrieving foreign keys for table $tableName...")
        metadata.getExportedKeys(catalog, null, tableName).toForeignKeys groupBy (_.fkTableName)
      }: _*)

      // transform the table mappings into class information
      tables map { tableName =>
        logger.info(s"Importing table $tableName...")
        val columns = metadata.getColumns(catalog, null, tableName, null).toColumns
        val primaryKeys = metadata.getPrimaryKeys(null, null, tableName).toPrimaryKeys

        TableModel(
          tableName,
          packageName = catalog.toLowerCase,
          className = tableName.toBigCamel,
          foreignKeys.getOrElse(tableName, Nil),
          columnModels = createColumnModels(columns, primaryKeys).sortBy(_.ordinalPosition))
      }
    }
  }

  /**
   * Creates the column models for the given columns, primary keys and foreign keys
   * @param columns the given table columns
   * @param primaryKeys the given collection of primary keys
   * @return a collection of column models
   */
  private[jdbc] def createColumnModels(columns: Seq[Column],
                                       primaryKeys: Seq[PrimaryKey]): Seq[ColumnModel] = {
    val primaryKeyMap = Map(primaryKeys map (pk => (pk.columnName, pk)): _*)
    columns map { c =>
      import c._
      ColumnModel(
        columnName,
        fieldName = columnName.toSmallCamel,
        typeName,
        primaryKey = primaryKeyMap.get(columnName),
        autoincrement,
        columnSize,
        ordinalPosition)
    }
  }

  /**
   * Opens a database connection
   * @param props the given configuration properties
   * @return a database [[Connection]]
   */
  private def openConnection(props: java.util.Properties): Connection = {
    val url = Option(props.getProperty("url")).orDie("Required property 'url' not found")
    Option(java.sql.DriverManager.getConnection(url, props)).orDie(s"Unable to establish connection to $url")
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
   * Represents Scala-Slick table model
   * @param tableName the name of the table being represented
   * @param className the name of the class being represented
   * @param packageName the name of the package the model class resides within
   * @param columnModels the given column models (database/class column definitions)
   */
  case class TableModel(tableName: String, packageName: String, className: String, foreignKeys: Seq[ForeignKey], columnModels: Seq[ColumnModel]) {
    private val columnModelMap = Map(columnModels.map(cm => (cm.columnName, cm)): _*)

    def get(columnName: String): Option[ColumnModel] = columnModelMap.get(columnName)

  }

  /**
   * Represents a Scala-Slick table column model
   * @param columnName the name of the column
   * @param fieldName the name of the member variable
   * @param typeName the Scala type name (e.g. "Int")
   * @param columnSize the defined column size
   * @param ordinalPosition the original position of the column within the table
   */
  case class ColumnModel(columnName: String, fieldName: String, typeName: String, primaryKey: Option[PrimaryKey],
                         autoincrement: Boolean, columnSize: Int, ordinalPosition: Int)

  /**
   * Represents a table column
   * @param columnName  the name of the column
   * @param typeName the given SQL type name (e.g. "BIGINT")
   * @param columnSize the column size
   * @param ordinalPosition the ordinal position of the column within a row
   * @param autoincrement indicates whether the column is auto-incremented
   * @param nullable indicates whether the column is nullable
   */
  case class Column(columnName: String, typeName: String, columnSize: Int, ordinalPosition: Int,
                    autoincrement: Boolean, nullable: Boolean)

  /**
   * Represents a foreign key constraint
   */
  case class ForeignKey(fkName: String,
                        pkTableName: String, pkColumnName: String,
                        fkTableName: String, fkColumnName: String,
                        deleteRule: Int, updateRule: Int, keySeq: Int)

  /**
   * Represents a primary key constraint
   */
  case class PrimaryKey(pkName: String, tableName: String, columnName: String, keySeq: Int)

  /**
   * Result Set Conversions
   * @param rs the given [[ResultSet]]
   */
  implicit class ResultSetConversions(val rs: ResultSet) extends AnyVal {

    /**
     * Transforms the results into a collection of columns
     * @return a collection of columns
     */
    def toColumns: Seq[Column] = {
      transform { rs =>
        val columnName = rs.getString("COLUMN_NAME")
        val dataType = rs.getInt("DATA_TYPE")
        val sqlTypeName = rs.getString("TYPE_NAME")
        val columnSize = rs.getInt("COLUMN_SIZE")
        val ordinalPosition = rs.getInt("ORDINAL_POSITION")
        val autoincrement = "YES" == rs.getString("IS_AUTOINCREMENT")
        val nullable = "YES" == rs.getString("IS_NULLABLE")
        Column(columnName, toDataType(sqlTypeName, dataType, nullable), columnSize, ordinalPosition, autoincrement, nullable)
      }.sortBy(_.ordinalPosition)
    }

    /**
     * Returns the appropriate Java type for the given SQL-based data type
     * @param sqlTypeName the given SQL type name (e.g. "BIGINT")
     * @param dataType the given JDBC SQL type
     * @param nullable indicates whether the type is nullable
     * @return the appropriate Java type
     */
    private def toDataType(sqlTypeName: String, dataType: Int, nullable: Boolean): String = {
      DataTypeMapping.get(dataType) map { myTypeName =>
        if (nullable) s"Option[$myTypeName]" else myTypeName
      } orDie s"Type '$sqlTypeName' (type index = $dataType) not recognized"
    }

    /**
     * Transforms the result set into a collection of foreign key objects
     * @return a collection of [[ForeignKey]] objects
     */
    def toForeignKeys: Seq[ForeignKey] = {
      transform { rs =>
        val fkName = rs.getString("FK_NAME")
        val fkTableName = rs.getString("FKTABLE_NAME")
        val fkColumnName = rs.getString("FKCOLUMN_NAME")
        val pkTableName = rs.getString("PKTABLE_NAME")
        val pkColumnName = rs.getString("PKCOLUMN_NAME")
        val deleteRule = rs.getInt("DELETE_RULE")
        val updateRule = rs.getInt("UPDATE_RULE")
        val keySeq = rs.getInt("KEY_SEQ")
        ForeignKey(fkName, pkTableName, pkColumnName, fkTableName, fkColumnName, deleteRule, updateRule, keySeq)
      }.sortBy(_.keySeq)
    }

    /**
     * Transforms the results into a mapping of key-value pairs
     * @return a mapping of key-value pairs
     */
    def toMap: Map[String, AnyRef] = Map(getColumnNames(rs) map (label => (label, rs.getObject(label))): _*)

    /**
     * Transforms the result set into a collection of primary key objects
     * @return a collection of [[PrimaryKey]] objects
     */
    def toPrimaryKeys: Seq[PrimaryKey] = {
      transform { rs =>
        val pkName = rs.getString("PK_NAME")
        val tableName = rs.getString("TABLE_NAME")
        val columnName = rs.getString("COLUMN_NAME")
        val keySeq = rs.getInt("KEY_SEQ")
        PrimaryKey(pkName, tableName, columnName, keySeq)
      }.sortBy(_.keySeq)
    }

    /**
     * Transforms a given row of results into typed results
     * @param f the transformation function
     * @tparam T the return type
     * @return the collection of typed results
     */
    def transform[T](f: ResultSet => T): Seq[T] = {
      val buf = mutable.Buffer[T]()
      while (rs.next()) buf += f(rs)
      buf
    }

    /**
     * Extracts the column names from the result set metadata
     * @param rs the given [[ResultSet]]
     * @return the collection of column names
     */
    private def getColumnNames(rs: ResultSet): Seq[String] = {
      val metadata = rs.getMetaData
      (1 to metadata.getColumnCount) map metadata.getColumnName
    }
  }

  /**
   * String Conversion Utility Functions
   * @param noun the given host string
   */
  implicit class StringConversions(val noun: String) extends AnyVal {

    /**
     * Adds a new line at the start of the given string expression
     * @return the given string expression with a new line added or the input string
     */
    def paragraph: String = if (noun.trim.nonEmpty) "\n" + noun else noun

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
     * Converts the given named identifier to capital camel case (e.g. "TheBigRedBall")
     * @return the named identifier as big camel case
     */
    def toBigCamel: String = {
      noun match {
        case s if s.contains("_") => s.split("[_]") map (_.toLowerCase) filter(_.nonEmpty) map (s => s.head.toUpper + s.tail) mkString
        case s if s.forall(_.isUpper) => s.head.toUpper + s.tail.toLowerCase
        case s => s.head.toUpper + s.tail
      }
    }

    /**
     * Converts the given named identifier to lowercase camel case (e.g. "theBigRedBall")
     * @return the named identifier as small camel case
     */
    def toSmallCamel: String = {
      noun match {
        case s if s.length > 1 && s.contains('_') =>
          val items = s.split("[_]") map (_.toLowerCase) filter(_.nonEmpty)
          (items.head ++ items.tail.map(s => s.head.toUpper + s.tail)) mkString
        case s if s.forall(_.isUpper) => s.toLowerCase
        case s =>
          indexOfLastUpperCase() map (p => s.substring(0, p).toLowerCase + s.substring(p)) getOrElse s.head.toLower + s.tail
      }
    }

    def indexOfLastUpperCase(start: Int = 0): Option[Int] = {
      val ca = noun.toCharArray
      var p = start
      while (p + 1 < ca.length) {
        if (!ca(p + 1).isUpper) return Some(p)
        p += 1
      }
      None
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

}