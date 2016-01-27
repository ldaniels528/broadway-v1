package com.github.ldaniels528.broadway.core.scope

import com.ldaniels528.commons.helpers.OptionHelper._

/**
  * Inherited Scope
  */
case class InheritedScope(parentScope: Scope) extends DefaultScope {

  override def find(name: String) = super.find(name) ?? parentScope.find(name)

  override def putIfAbsent(values: Seq[(String, Any)]) = {
    values foreach { case (name, value) =>
      if(find(name).nonEmpty) super.putIfAbsent(Seq(name -> value))
    }
  }

}
