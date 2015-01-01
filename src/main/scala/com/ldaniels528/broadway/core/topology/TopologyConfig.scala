package com.ldaniels528.broadway.core.topology

/**
 * Topology Configuration
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class TopologyConfig(locations: Seq[Location],
                          propertySets: Seq[PropertySet],
                          topologies: Seq[TopologyDescriptor])