package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Data

/**
  * Created by ldaniels on 1/23/16.
  */
trait DataWriting {

  def write(data: Data): Int

}
