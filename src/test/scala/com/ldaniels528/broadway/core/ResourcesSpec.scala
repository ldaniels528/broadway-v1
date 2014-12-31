package com.ldaniels528.broadway.core

import com.ldaniels528.broadway.core.Resources.ClasspathResource
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}
import org.scalatest.Matchers._

import scala.io.Source

/**
 * Broadway Resources Spec
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ResourcesSpec() extends FeatureSpec with GivenWhenThen with MockitoSugar {

  info("As a Resource")
  info("I want to be able to represent pointers to content, including remote files")

  feature("Content can be retrieve from a classpath resource") {
    scenario("Retrieve the contents of a file within the classpath") {
      Given("a the classpath resource to a file")
      val resource = ClasspathResource("/server-config.properties")

      When("the content is retrieved")
      val contents = resource.getInputStream map(in => Source.fromInputStream(in)) map(_.getLines().mkString)

      Then("the content should match the exact result")
      contents shouldBe Some("broadway.directories.base=/Users/ldaniels/broadway")
    }
  }

}
