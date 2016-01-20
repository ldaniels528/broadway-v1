package com.github.ldaniels528.broadway.core.io.layout

/**
  * Represents the logic layout of a text format
  */
trait Layout {

  def id: String

  def header: Seq[Division]

  def footer: Seq[Division]

}