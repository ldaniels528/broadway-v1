package com.ldaniels528.broadway.core.resources

/**
 * Represents a iterable resource
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait IterableResource[T] extends Resource {

  def iterator: Iterator[T]

}
