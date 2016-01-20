package com.github.ldaniels528.broadway.core.opcode

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.device.Device

/**
  * Represents a device I/O count opCode
  */
class DeviceIOCountOpCode (device: Device) extends OpCode {

  override def eval(rt: RuntimeContext) = Some(device.count)

}
