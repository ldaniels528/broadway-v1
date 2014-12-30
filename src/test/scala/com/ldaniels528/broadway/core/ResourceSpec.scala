package com.ldaniels528.broadway.core

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}

/**
 * Broadway Resource Spec
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ResourceSpec() extends FeatureSpec with GivenWhenThen with MockitoSugar {

  info("As a FlowTopology instance")
  info("I want to be able to represent topology flows at runtime")

  feature("Load an XML flow and transform it into an object graph") {
    scenario("Load an XML flow from the classpath and transform it into an object graph") {
      Given("A the resource path of an XML flow")
      val resourcePath = "/nasdaqTopology.xml"

      When("We load the XML flow topology")
      val topology = Nil // ClasspathResource(resourcePath).getInputStream use TopologyDescriptor.parse

      Then("The flow topology object graph should match the exact result")
      info(s"topology: $topology")

      /*
      val actorTags = List(
        ConsumerTag("symbolLookupActor", "com.ldaniels528.broadway.test.nasdaq.TestNasdaqData.StockQuoteLookupActor",
          props = new Properties(),
          onSuccess = Some(SourceTargetTag("actor", "kafkaTopicActor"))),
        ConsumerTag("kafkaTopicActor", "com.ldaniels528.broadway.core.actors.KafkaAvroPublishingActor",
          props = Map(
            "topic" -> "shocktrade.quotes.yahoo.avro",
            "brokers" -> "dev501:9091,dev501:9092,dev501:9093,dev501:9094,dev501:9095,dev501:9096").toProps,
          onSuccess = None))
      val resourceTags = List(
        ResourceTag("nasdaqFile", "classpath", "/data/NASDAQ.txt"))
      val spoutTags = List(
        ProducerTag("nasdaqData", "com.ldaniels528.broadway.core.spouts.FileReaderOpWithDelimiter",
          SourceTargetTag("resource", "nasdaqFile"),
          SourceTargetTag("actor", "symbolLookupActor")))
      topology shouldBe TopologyDescriptor(actorTags, resourceTags, spoutTags)*/
    }
  }

}
