package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.axi4._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class SRAMHelper extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val raddr = Input(UInt(32.W))
    val ren   = Input(Bool())
    val rdata = Output(UInt(32.W))
    // 新增写端口
    val waddr = Input(UInt(32.W))
    val wen   = Input(Bool())
    val wdata = Input(UInt(32.W))
  })
  setInline("SRAMHelper.v",
    """module SRAMHelper(
      |  input [31:0] raddr,
      |  input ren,
      |  output reg [31:0] rdata,
      |  input [31:0] waddr,
      |  input wen,
      |  input [31:0] wdata
      |);
      |import "DPI-C" function void dpi_sram_read(input int raddr, output int rdata);
      |import "DPI-C" function void dpi_sram_write(input int waddr, input int wdata);
      |always @(*) begin
      |  if (wen)
      |    dpi_sram_write(waddr, wdata);
      |  if (ren)
      |    dpi_sram_read(raddr, rdata);
      |  else
      |    rdata = 0;
      |end
      |endmodule
    """.stripMargin)
}

class AXI4SRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val beatBytes = 4
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
        address       = address,
        executable    = true,
        supportsWrite = TransferSizes(1, beatBytes),
        supportsRead  = TransferSizes(1, beatBytes),
        interleavedId = Some(0))
    ),
    beatBytes  = beatBytes)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)

    val sram = Module(new SRAMHelper)

    // 读通道
    val (stateIdle, stateWaitRready) = (0.U, 1.U)
    val state = RegInit(stateIdle)
    state := Mux(state === stateIdle,
               Mux(in.ar.fire, stateWaitRready, stateIdle),
               Mux(in. r.fire, stateIdle, stateWaitRready))

    sram.io.raddr := in.ar.bits.addr
    sram.io.ren := in.ar.fire
    in.ar.ready := (state === stateIdle)
//    assert(!(in.ar.fire && in.ar.bits.size === 3.U), "do not support 8 byte transfter")

    in.r.bits.data := RegEnable(sram.io.rdata, in.ar.fire)
    in.r.bits.id := RegEnable(in.ar.bits.id, in.ar.fire)
    in.r.bits.resp := 0.U
    in.r.bits.last := true.B
    in.r.valid := (state === stateWaitRready)

    // 写通道
    val (wIdle, wWaitB) = (0.U, 1.U)
    val wState = RegInit(wIdle)
    wState := Mux(wState === wIdle,
                  Mux(in.aw.fire && in.w.fire, wWaitB, wIdle),
                  Mux(in.b.fire, wIdle, wWaitB))

    in.aw.ready := (wState === wIdle)
    in.w.ready  := (wState === wIdle)
    in.b.valid  := (wState === wWaitB)

    sram.io.waddr := in.aw.bits.addr
    sram.io.wdata := in.w.bits.data
    sram.io.wen   := in.aw.fire && in.w.fire
  }
}
