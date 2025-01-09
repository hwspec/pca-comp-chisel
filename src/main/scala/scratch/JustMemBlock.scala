package scratch

import chisel3._
import chisel3.util._
import common.GenVerilog

class JustMemBlock(wordbw: Int = 64, nwords : Int = 8192) extends Module {
  val io = IO(new Bundle {
    val we = Input(Bool())
    val inAddr = Input(UInt(log2Ceil(nwords).W))
    val inData = Input(UInt(wordbw.W))
    val outData = Output(UInt(wordbw.W))
  })

  io.outData := 0.U

  val mem = SyncReadMem(nwords, UInt(wordbw.W))

  when(io.we) {
    mem.write(io.inAddr, io.inData)
  }
  io.outData := mem.read(io.inAddr, true.B)
}

object JustMemBlock extends App {
  GenVerilog.generate(new JustMemBlock())
}
