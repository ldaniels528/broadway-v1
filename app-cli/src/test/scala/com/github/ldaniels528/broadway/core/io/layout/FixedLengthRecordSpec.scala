package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.record.impl.FixedLengthRecord
import com.github.ldaniels528.broadway.core.io.record.{DataTypes, Field}
import com.ldaniels528.commons.helpers.OptionHelper.Risky._
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}

/**
  * Fixed-Length Record Specification 
  */
class FixedLengthRecordSpec() extends FeatureSpec with BeforeAndAfterEach with GivenWhenThen with MockitoSugar {

  info("As a FixedLengthRecord instance")
  info("I want to be able to transform text into a fixed-length string (and vice versa)")

  feature("Transforms text to fixed-length records") {
    scenario("Import a fixed-length stock quote into a fixed-length record") {
      Given("a text string in fixed-length format")
      val line = "AAPL      96.76     96.99     95.89     109.99"

      And("a fixed-length record")
      val record = FixedLengthRecord(
        id = "fixed_rec",
        fields = Seq(
          Field(name = "symbol", `type` = DataTypes.STRING, length = 10),
          Field(name = "open", `type` = DataTypes.STRING, length = 10),
          Field(name = "close", `type` = DataTypes.STRING, length = 10),
          Field(name = "low", `type` = DataTypes.STRING, length = 10),
          Field(name = "high", `type` = DataTypes.STRING, length = 10)
        ))

      When("the text is consumed")
      record.fromLine(line)

      Then("the toLine method should return the fixed-length string")
      info(record.toLine)
      record.toLine.trim shouldBe line

      And(s"the record must contain the values")
      val validation = List("symbol" -> "AAPL", "open" -> "96.76", "close" -> "96.99", "low" -> "95.89", "high" -> "109.99")
      record.fields foreach { field =>
        info(s"name: ${field.name}, value: ${field.value}")
      }
      record.fields.map(f => f.name -> f.value) shouldBe validation.map { case (k, v) => (k, Some(v)) }
    }
  }

}