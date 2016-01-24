package com.github.ldaniels528.broadway.core.io.layout.json

import com.github.ldaniels528.broadway.core.io.layout.{FieldSet, Layout}

/**
  * MongoDB Layout
  */
case class MongoDbLayout(id: String, fieldSet: FieldSet) extends Layout