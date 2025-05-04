package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.apb._
import freechips.rocketchip.system.SimAXIMem

object AXI4SlaveNodeGenerator {
  def apply(params: Option[MasterPortParams], address: Seq[AddressSet])(implicit
      valName: ValName
  ) =
    AXI4SlaveNode(
      params
        .map(p =>
          AXI4SlavePortParameters(
            slaves = Seq(
              AXI4SlaveParameters(
                address = address,
                executable = p.executable,
                supportsWrite = TransferSizes(1, p.maxXferBytes),
                supportsRead = TransferSizes(1, p.maxXferBytes)
              )
            ),
            beatBytes = p.beatBytes
          )
        )
        .toSeq
    )
}

class ysyxSoCASIC(implicit p: Parameters) extends LazyModule {
  val xbar = AXI4Xbar()
  val apbxbar = LazyModule(new APBFanout).node
  val cpu = LazyModule(new CPU(idBits = ChipLinkParam.idBits))
  val chipMaster = None
  val chiplinkNode = None

  val luart = LazyModule(
    new APBUart16550(AddressSet.misaligned(0x10000000, 0x1000))
  )
  val lgpio = LazyModule(
    new APBGPIO(AddressSet.misaligned(0x10002000, 0x10))
  )
  val lbram = LazyModule(
    new AXI4BRAM(AddressSet.misaligned(0x20000000L, 0x1000000))
  )
  val lsram = LazyModule(
    new APBSRAM(AddressSet.misaligned(0x80000000L, 0x1000000))
  )

  List(luart.node, lgpio.node, lsram.node).map(_ := apbxbar)
  List(apbxbar := AXI4ToAPB(), lbram.node).map(_ := xbar)
  xbar := cpu.masterNode

  override lazy val module = new Impl
  class Impl extends LazyModuleImp(this) with DontTouch {
    // generate delayed reset for cpu, since chiplink should finish reset
    // to initialize some async modules before accept any requests from cpu
    cpu.module.reset := SynchronizerShiftReg(reset.asBool, 10) || reset.asBool

    val fpga_io = None

    // connect interrupt signal to cpu
    val intr_from_chipSlave = IO(Input(Bool()))
    cpu.module.interrupt := intr_from_chipSlave

    // expose slave I/O interface as ports
    val uart = IO(chiselTypeOf(luart.module.uart))
    val gpio = IO(chiselTypeOf(lgpio.module.gpio_bundle))
    val sram = IO(chiselTypeOf(lsram.module.sram_bundle))
    uart <> luart.module.uart
    gpio <> lgpio.module.gpio_bundle
    sram <> lsram.module.sram_bundle
  }
}

class ysyxSoCFull(implicit p: Parameters) extends LazyModule {
  val asic = LazyModule(new ysyxSoCASIC)
  ElaborationArtefacts.add("graphml", graphML)

  override lazy val module = new Impl
  class Impl extends LazyModuleImp(this) with DontTouch {
    val masic = asic.module

    masic.intr_from_chipSlave := false.B

    val externalPins = IO(new Bundle {
      val uart = chiselTypeOf(masic.uart)
      val gpio = chiselTypeOf(masic.gpio)
      val sram = chiselTypeOf(masic.sram)
    })
    externalPins.uart <> masic.uart
    externalPins.gpio <> masic.gpio
    externalPins.sram <> masic.sram
  }
}
