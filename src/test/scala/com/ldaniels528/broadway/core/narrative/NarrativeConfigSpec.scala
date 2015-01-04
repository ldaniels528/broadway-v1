package com.ldaniels528.broadway.core.narrative

import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}

/**
 * Topology Configuration Specification
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class NarrativeConfigSpec() extends FeatureSpec with GivenWhenThen with MockitoSugar {

  info("As a TopologyConfig")
  info("I want to be able to represent Broadway topologies at run-time")

  feature("Feeds can have dependencies") {
    scenario("Feeds should only be processed when all dependencies are satisfied") {
      Given("a topology runtime")
      implicit val rt = new NarrativeRuntime()

      And("a collection of feeds with inter-dependencies")
      val feedA = FeedDescriptor(name = "NASDAQ.txt", matching = "exact")
      val feedB = FeedDescriptor(name = "NYSE.txt", matching = "exact", dependencies = Seq(feedA))
      val feedC = FeedDescriptor(name = "AMEX.txt", matching = "exact")
      val feedD = FeedDescriptor(name = "OTCBB.txt", matching = "exact", dependencies = Seq(feedA, feedC))
      val descriptors = Seq(feedA, feedB, feedC, feedD)

      When("the feeds have been realized")
      val feeds = descriptors map (_.toFeed(rt))
      val mappings = Map(Seq("feedA", "feedB", "feedC", "feedD") zip feeds: _*)
      showFeeds(mappings)
      info("")

      // attempt to process each feed
      (1 to 4) foreach { _ =>
        mappings.find { case (name, feed) => feed.ready && !feed.processed} foreach { case (name, feed) =>
          When(s"$name has been processed")
          feed.processed = true
          showFeeds(mappings)
          info("")
        }
      }

      Then("all feeds should be processed")
      mappings.forall { case (name, feed) => feed.processed} shouldBe true
    }
  }

  private def showFeeds(mappings: Map[String, Feed]) = {
    mappings foreach { case (name, feed) =>
      info(s"$name is ${isReady(feed)}")
    }
  }

  private def isReady(feed: Feed) = {
    if (feed.processed) "processed"
    else if (feed.ready) "ready"
    else "not ready"
  }

}
