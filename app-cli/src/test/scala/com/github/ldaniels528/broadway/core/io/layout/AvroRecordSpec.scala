package com.github.ldaniels528.broadway.core.io.layout


import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import play.api.libs.json.Json

/**
  * Avro Record Specification
  */
class AvroRecordSpec() extends FeatureSpec with BeforeAndAfterEach with GivenWhenThen with MockitoSugar {

  info("As a AvroRecord instance")
  info("I want to be able to transform JSON into Avro record (and vice versa)")

  feature("Transform JSON text to Avro record") {
    scenario("Import a JSON stock quote into a Avro record") {
      Given("a text string in JSON format")
      val jsonString = """{ "symbol":"AAPL", "open":96.76, "close":96.99, "low":95.89, "high":109.99 }"""

      And("a Avro record")
      val record = AvroRecord(
        id = "EodCompanyInfo",
        namespace = "com.shocktrade.avro",
        fields = Seq(
          Field(name = "symbol", `type` = DataTypes.STRING),
          Field(name = "open", `type` = DataTypes.DOUBLE),
          Field(name = "close", `type` = DataTypes.DOUBLE),
          Field(name = "low", `type` = DataTypes.DOUBLE),
          Field(name = "high", `type` = DataTypes.DOUBLE)
        ), `type` = RecordTypes.BODY)

      When("the Avro Schema is queried:")
      info(s"The Avro Schema is ${Json.prettyPrint(Json.parse(record.toSchemaString))}")

      And("the JSON string is consumed")
      record.fromJson(jsonString)

      Then("the toJson method should return the JSON string")
      info(record.toJson.toString())
      record.toJson.toString() shouldBe """{"symbol":"AAPL","open":96.76,"close":96.99,"low":95.89,"high":109.99}"""

      And(s"the record must contain the values")
      val validation = List("symbol" -> "AAPL", "open" -> 96.76d, "close" -> 96.99d, "low" -> 95.89d, "high" -> 109.99d)
      record.fields foreach { field =>
        info(s"name: ${field.name}, value: ${field.value}")
      }
      record.fields.map(f => f.name -> f.value) shouldBe validation.map { case (k, v) => k -> Some(v) }
    }
  }

}