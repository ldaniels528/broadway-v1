package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.flow.Flow

/**
  * ETL Configuration
  *
  * @author lawrence.daniels@gmail.com
  */
case class ETLConfig(id: String, flows: Seq[Flow])