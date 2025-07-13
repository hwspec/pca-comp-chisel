package pca

import chisel3._
import chisel3.util._
import common.GenVerilog

class SRAM1RW(depth: Int, width: Int, id : Int, useSyncReadMem: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())                     // enable read/write
    val we     = Input(Bool())                     // write enable
    val addr   = Input(UInt(log2Ceil(depth).W))    // address
    val wdata  = Input(UInt(width.W))              // write data
    val rdata  = Output(UInt(width.W))             // read data (valid 1 cycle after read)
  })
  override def desiredName =
    if (useSyncReadMem)
      s"${this.getClass.getSimpleName}__depth${depth}_width${width}_simulation"
    else
      s"${this.getClass.getSimpleName}__depth${depth}_width${width}_blackbox"

  if (useSyncReadMem) {
    val mem = SyncReadMem(depth, UInt(width.W))

    io.rdata := 0.U
    when(io.en) {
      when(io.we) {
        mem.write(io.addr, io.wdata)
        // printf("SRAM: write addr=%d data=%d\n", io.addr, io.wdata)
      }
      val rdata = Wire(UInt(width.W))
      rdata := mem.read(io.addr, true.B)
      io.rdata := rdata
      // printf("SRAM: read addr=%d data=%d\n", io.addr, rdata)
    }
  } else {
    val sram = Module(new BlackBoxSRAM1RW(depth, width, id))  // replace with SRAM module
    sram.io.clk   := clock
    sram.io.en    := io.en
    sram.io.we    := io.we
    sram.io.addr  := io.addr
    sram.io.wdata := io.wdata
    io.rdata      := sram.io.rdata
  }
}

class BlackBoxSRAM1RW(depth: Int, width: Int, id: Int) extends
  BlackBox(Map("DEPTH" -> depth, "WIDTH" -> width)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk    = Input(Clock())
    val en     = Input(Bool())
    val we     = Input(Bool())
    val addr   = Input(UInt(log2Ceil(depth).W))
    val wdata  = Input(UInt(width.W))
    val rdata  = Output(UInt(width.W))
  })

  setInline("BlackBoxSRAM1RW.v",
    s"""
       |module BlackBoxSRAM1RW #(parameter DEPTH=$depth, WIDTH=$width)(
       |  input clk,
       |  input en,
       |  input we,
       |  input [${log2Ceil(depth) - 1}:0] addr,
       |  input [${width - 1}:0] wdata,
       |  output reg [${width - 1}:0] rdata
       |);
       |  reg [WIDTH-1:0] mem [0:DEPTH-1];
       |  always @(posedge clk) begin
       |    if (en) begin
       |      if (we) begin
       |        mem[addr] <= wdata;
       |      end else begin
       |        rdata <= mem[addr];
       |      end
       |    end
       |  end
       |endmodule
    """.stripMargin)
}


object SRAM1RW extends App {
  GenVerilog(new SRAM1RW(256, 128, id = 0, true))
  GenVerilog(new SRAM1RW(256, 128, id = 0, false))
}
