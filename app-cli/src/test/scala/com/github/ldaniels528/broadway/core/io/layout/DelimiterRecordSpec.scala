package com.github.ldaniels528.broadway.core.io.layout


import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}

/**
  * Delimiter Record Spec
  */
class DelimiterRecordSpec() extends FeatureSpec with BeforeAndAfterEach with GivenWhenThen with MockitoSugar {

  info("As a DelimiterRecord instance")
  info("I want to be able to transform text into delimited record (and vice versa)")

  feature("Transform delimited text to delimited record") {
    scenario("Import a delimited stock quote into a delimited record") {
      Given("a text string in delimited format")
      val line = "AAPL\t96.76\t96.99\t95.89\t109.99"

      And("a delimited record")
      val record = DelimitedRecord(
        id = "delim_rec",
        delimiter = "\t",
        fields = Seq(
          Field(name = "symbol", `type` = DataTypes.STRING),
          Field(name = "open", `type` = DataTypes.STRING),
          Field(name = "close", `type` = DataTypes.STRING),
          Field(name = "low", `type` = DataTypes.STRING),
          Field(name = "high", `type` = DataTypes.STRING)
        ))

      When("the text is consumed")
      record.fromLine(line)

      Then("the toLine method should return the delimited string")
      info(record.toLine)
      record.toLine shouldBe "AAPL\t96.76\t96.99\t95.89\t109.99"

      And(s"the record must contain the values")
      val validation = List("symbol" -> "AAPL", "open" -> "96.76", "close" -> "96.99", "low" -> "95.89", "high" -> "109.99")
      record.fields foreach { field =>
        info(s"name: ${field.name}, value: ${field.value}")
      }
      record.fields.map(f => f.name -> f.value) shouldBe validation.map { case (k, v) => (k, Some(v)) }
    }
  }

}