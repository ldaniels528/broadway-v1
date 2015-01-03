package com.ldaniels528.broadway.core.topology

import java.util.Properties

/**
 * Represents a Broadway Topology
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class TopologyDescriptor(id: String, className: String, properties: TopologyRuntime => Properties)
