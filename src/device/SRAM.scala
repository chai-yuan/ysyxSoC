package ysyx

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class SRAMIO extends Bundle {
  val addr = Output(UInt(18.W))
  val data = Analog(32.W)
  val ce = Output(UInt(2.W))
  val oe = Output(UInt(2.W))
  val we = Output(UInt(2.W))
  val lb = Output(UInt(2.W))
  val ub = Output(UInt(2.W))
}

class sram_top_axi extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in = Flipped(
      new AXI4Bundle(
        AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4)
      )
    )
    val sram = new SRAMIO
  })
}

class sram_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in =
      Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val sram = new SRAMIO
  })
}

class sram extends BlackBox {
  val io = IO(Flipped(new SRAMIO))
}

class sramChisel extends RawModule {
  val io = IO(Flipped(new SRAMIO))
}

class AXI4SRAM(address: Seq[AddressSet])(implicit p: Parameters)
    extends LazyModule {
  val beatBytes = 4
  val node = AXI4SlaveNode(
    Seq(
      AXI4SlavePortParameters(
        Seq(
          AXI4SlaveParameters(
            address = address,
            executable = true,
            supportsWrite = TransferSizes(1, beatBytes),
            supportsRead = TransferSizes(1, beatBytes),
            interleavedId = Some(0)
          )
        ),
        beatBytes = beatBytes
      )
    )
  )

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val sram_bundle = IO(new SRAMIO)

    val msram = Module(new sram_top_axi)
    msram.io.clock := clock
    msram.io.reset := reset.asBool
    msram.io.in <> in
    sram_bundle <> msram.io.sram
  }
}

class APBSRAM(address: Seq[AddressSet])(implicit p: Parameters)
    extends LazyModule {
  val node = APBSlaveNode(
    Seq(
      APBSlavePortParameters(
        Seq(
          APBSlaveParameters(
            address = address,
            executable = true,
            supportsRead = true,
            supportsWrite = true
          )
        ),
        beatBytes = 4
      )
    )
  )

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val sram_bundle = IO(new SRAMIO)

    val msram = Module(new sram_top_apb)
    msram.io.clock := clock
    msram.io.reset := reset.asBool
    msram.io.in <> in
    sram_bundle <> msram.io.sram
  }
}
