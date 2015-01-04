package com.ldaniels528.broadway.server

import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.server.ServerConfig._
import com.ldaniels528.trifecta.util.PropertiesHelper._
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}

/**
 * Server Config Parser Spec
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ServerConfigParserSpec() extends FeatureSpec with GivenWhenThen with MockitoSugar {

  info("As a ServerConfigParser")
  info("I want to be able to parse server configurations")

  feature("Server configuration can be parsed from a classpath resource") {
    scenario("Parse a server configuration from a classpath resource") {
      Given("a the classpath resource")
      val resource = ClasspathResource("/broadway-config.xml")

      When("the server configuration is parsed")
      val config = ServerConfigParser.parse(resource)

      Then("the server configuration should match the exact result")
      config shouldBe Some(ServerConfig(
        props = Map("broadway.directories.base" -> "/Users/ldaniels/broadway").toProps,
        httpInfo = Some(HttpInfo(host = "0.0.0.0", port = 9999))
      ))

    }
  }

}