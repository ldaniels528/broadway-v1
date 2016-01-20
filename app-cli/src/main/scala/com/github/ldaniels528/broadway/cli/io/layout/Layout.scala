package com.github.ldaniels528.broadway.cli.io.layout

/**
  * Represents the logic layout of a text format
  */
trait Layout {

  def id: String

  def header: Option[Header]

  def footer: Option[Footer]

}