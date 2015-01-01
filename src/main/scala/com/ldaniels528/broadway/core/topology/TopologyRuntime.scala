package com.ldaniels528.broadway.core.topology

import scala.collection.concurrent.TrieMap

/**
 * Represents a topology runtime
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class TopologyRuntime() {
  val feeds = TrieMap[String, Feed]()

}
