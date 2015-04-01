package com.ldaniels528.broadway.core.resources

/**
 * Represents a generic resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait Resource extends Serializable {

  /**
   * Returns an option of the resource name
   * @return an option of the resource name
   */
  def getResourceName: Option[String]

}
