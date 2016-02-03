package com.github.ldaniels528.broadway.core.io.filters

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * An AngularJS-style filtering mechanism for fields
  * @author lawrence.daniels@gmail.com
  */
trait Filter {

  def execute(value: Option[Any], args: List[String])(implicit scope: Scope): Option[Any]

}
