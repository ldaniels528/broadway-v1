package com.ldaniels528.broadway.core.narrative

import com.ldaniels528.broadway.core.resources._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}

/**
 * Topology Configuration Parser Specification
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class NarrativeConfigParserSpec() extends FeatureSpec with GivenWhenThen with MockitoSugar {

  info("As a NarrativeConfigParser")
  info("I want to be able to represent Broadway narrative at run-time")

  feature("Topology configurations can be parsed from a classpath resource") {
    scenario("Parse a Narrative configuration from a classpath resource") {
      Given("a the classpath resource")
      val resource = ClasspathResource("/shocktrade-nasdaq.xml")

      When("the narrative configuration is parsed")
      val config = NarrativeConfigParser.parse(resource)

      Then("the Topology configuration should match the exact result")
      info(s"config = $config")
      /*
      config shouldBe Some(TopologyConfig(
        locations = Nil,
        propertySets = Nil,
        topologies = List(
          TopologyDescriptor(id = "QuoteImportTopology", className = "com.shocktrade.topologies.NASDAQSymbolImportTopology",
            new PropertySet(
              id = "KafkaProps",
              props = Map(
                "topic" -> "shocktrade.quotes.yahoo.avro",
                "brokers" -> "dev501:9091,dev501:9092,dev501:9093,dev501:9094,dev501:9095,dev501:9096").toProps)))
      ))*/

    }
  }

}
