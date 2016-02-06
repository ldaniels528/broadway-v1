package com.github.ldaniels528.broadway.core.io.layout


import com.github.ldaniels528.broadway.core.io.record.impl.JsonRecord
import com.github.ldaniels528.broadway.core.io.record.{DataTypes, Field}
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}

/**
  * Json Record Specification
  */
class JsonRecordSpec() extends FeatureSpec with BeforeAndAfterEach with GivenWhenThen with MockitoSugar {

  info("As a JsonRecord instance")
  info("I want to be able to transform text into JSON record (and vice versa)")

  feature("Transform JSON text into a JSON record") {
    scenario("Import a JSON stock quote into a JSON record") {
      Given("a text string in JSON format")
      val text = """{ "symbol":"AAPL", "open":96.76, "close":96.99, "low":95.89, "high":109.99 }"""

      And("a JSON record")
      val record = JsonRecord(
        id = "json_rec",
        fields = Seq(
          Field(name = "symbol", path = "symbol", `type` = DataTypes.STRING),
          Field(name = "open", path = "open", `type` = DataTypes.STRING),
          Field(name = "close", path = "close", `type` = DataTypes.STRING),
          Field(name = "low", path = "low", `type` = DataTypes.STRING),
          Field(name = "high", path = "high", `type` = DataTypes.STRING)
        ))

      When("the text is consumed")
      record.fromText(text)

      Then("the toLine method should return the JSON string")
      info(record.toText)
      record.toText shouldBe """{"symbol":"AAPL","open":96.76,"close":96.99,"low":95.89,"high":109.99}"""

      And(s"the record must contain the values")
      val validation = List("symbol" -> "AAPL", "open" -> 96.76d, "close" -> 96.99d, "low" -> 95.89d, "high" -> 109.99d)
      record.fields foreach { field =>
        info(s"name: ${field.name}, value: ${field.value}")
      }
      record.fields.map(f => f.name -> f.value) shouldBe validation.map { case (k, v) => (k, Some(v)) }
    }
  }

}
