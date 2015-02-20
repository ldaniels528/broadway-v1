package com.ldaniels528.broadway.core.jdbc

import java.io.File

import com.ldaniels528.broadway.core.jdbc.MySQLtoSlickGenerator.{ModelClass, ModelField}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}
import org.scalatest.Matchers._

import scala.io.Source
import scala.util.Properties

/**
 * MySQL Catalog to Slick Class Generator
 * @author lawrence.daniels@gmail.com
 */
class MySQLtoSlickGeneratorSpec() extends FeatureSpec with GivenWhenThen with MockitoSugar {

  info("As a MySQLtoSlickGenerator")
  info("I want to be able to export a MySQL catalog as TypeSafe Slick sources")

  feature("Export a MySQL catalog as TypeSafe Slick sources") {
    scenario("Export TypeSafe Slick sources from a local MySQL server") {
      Given("a temporary output directory")
      val outputDirectory = new File(new File(Properties.tmpDir), "MySQLtoSlickGeneratorSpec")

      And("a collection of classes information instances")
      val classes = Seq(
        ModelClass("people", "dummy", "Person", Seq(
          ModelField(columnName = "person_id", fieldName = "personId", typeName = "Long", nullable = false, columnSize = 18, ordinalPosition = 1),
          ModelField(columnName = "first_name", fieldName = "firstName", typeName = "String", nullable = false, columnSize = 65, ordinalPosition = 2),
          ModelField(columnName = "last_name", fieldName = "lastName", typeName = "String", nullable = false, columnSize = 65, ordinalPosition = 3)
        ))
      )

      When("the classes are generated")
      MySQLtoSlickGenerator.generateSources(classes, outputDirectory)

      Then("the output file's content should match the expected result")
      outputDirectory.listFiles().length shouldBe 1

      val packageDirectory = new File(outputDirectory, "dummy")
      val content = Source.fromFile(new File(packageDirectory, "Person.scala")).getLines().mkString("\n")
      content.trim shouldBe
        """|package dummy
            |
            |import java.sql.Date
            |import scala.slick.driver.MySQLDriver.simple._
            |
            |case class Person(personId: Long, firstName: String, lastName: String)
            |
            |class Persons(tag: Tag) extends Table[(Long, String, String)](tag, "people") {
            |	def personId = column[Long]("person_id")
            |	def firstName = column[String]("first_name")
            |	def lastName = column[String]("last_name")
            |	def * = (personId, firstName, lastName)
            |}""".stripMargin('|').trim
    }
  }

}
