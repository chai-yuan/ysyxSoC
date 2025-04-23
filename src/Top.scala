package ysyx

import chisel3._
import org.chipsalliance.cde.config.{Parameters, Config}
import freechips.rocketchip.system._
import freechips.rocketchip.diplomacy.LazyModule

object Config {
  def hasChipLink: Boolean = false
  def sdramUseAXI: Boolean = false
}

class ysyxSoCTop extends Module {
  implicit val config: Parameters = new Config(
    new Edge32BitConfig ++ new DefaultRV32Config
  )

  val io = IO(new Bundle {})
  val dut = LazyModule(new ysyxSoCFull)
  val mdut = Module(dut.module)
  mdut.dontTouchPorts()
  mdut.externalPins := DontCare
}

class ysyxSoCFPGA extends Module {
  implicit val config: Parameters = new Config(
    new Edge32BitConfig ++ new DefaultRV32Config
  )

  val io = IO(new Bundle {
    val uart = new UARTIO
    val gpio = new GPIOIO
  })
  val dut = LazyModule(new ysyxSoCASIC)
  val mdut = Module(dut.module)
  mdut.dontTouchPorts()
  mdut.intr_from_chipSlave := false.B
  mdut.uart <> io.uart
  mdut.gpio <> io.gpio
}

object Elaborate extends App {
  val firtoolOptions = Array("-disable-all-randomization", "-strip-debug-info")
  circt.stage.ChiselStage.emitSystemVerilogFile(
    new ysyxSoCFPGA,
    args,
    firtoolOptions
  )
  // circt.stage.ChiselStage.emitSystemVerilogFile(new ysyxSoCTop, args, firtoolOptions)
}
