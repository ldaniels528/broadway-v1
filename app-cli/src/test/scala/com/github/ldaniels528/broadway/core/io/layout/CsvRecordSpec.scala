package com.github.ldaniels528.broadway.core.io.layout


import com.github.ldaniels528.broadway.core.io.record.{DataTypes, Field}
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}

/**
  * Comma Separated Values (CSV) Record Specification
  */
class CsvRecordSpec() extends FeatureSpec with BeforeAndAfterEach with GivenWhenThen with MockitoSugar {

  info("As a CsvRecord instance")
  info("I want to be able to transform text into CSV (and vice versa)")

  feature("Transform CSV text to CSV record") {
    scenario("Import a CSV stock quote into a CSV record") {
      Given("a text string in CSV format")
      val line = """"AAPL", 96.76, 96.99, 95.89, 109.99"""

      And("a CSV record")
      val record = CsvRecord(
        id = "cvs_rec",
        fields = Seq(
          Field(name = "symbol", `type` = DataTypes.STRING),
          Field(name = "open", `type` = DataTypes.STRING),
          Field(name = "close", `type` = DataTypes.STRING),
          Field(name = "low", `type` = DataTypes.STRING),
          Field(name = "high", `type` = DataTypes.STRING)
        ))

      When("the text is consumed")
      record.fromLine(line)

      Then("the toLine method should return the CSV string")
      info(record.toLine)
      record.toLine shouldBe """"AAPL","96.76","96.99","95.89","109.99""""

      And(s"the record must contain the values")
      val validation = List("symbol" -> "AAPL", "open" -> "96.76", "close" -> "96.99", "low" -> "95.89", "high" -> "109.99")
      record.fields foreach { field =>
        info(s"name: ${field.name}, value: ${field.value}")
      }
      record.fields.map(f => f.name -> f.value) shouldBe validation.map { case (k, v) => (k, Some(v)) }
    }
  }

}