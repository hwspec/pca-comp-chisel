package scratch

import hardfloat._
import chisel3._
import chisel3.util.{Cat, Decoupled, DecoupledIO, Queue}
//import chisel3.stage.ChiselStage
import _root_.circt.stage.ChiselStage // for 5.0.0

class FPTest(val expW: Int = 8, val sigW: Int = 24) extends Module {
  val bw: Int = expW + sigW
  val io = IO(new Bundle {
    val in  = Input(Bits(bw.W))
    val out = Output(Bits(bw.W))
  })

  val recfn = Wire(UInt((bw+1).W))
  recfn := recFNFromFN(expW, sigW, io.in)
  io.out := fNFromRecFN(expW, sigW, recfn)

  assert(io.out === io.in, "'out' should equal 'in'" )
}


class FPOPTest(val expW: Int = 8, val sigW: Int = 24, val mode: FPOPTest.Mode = FPOPTest.ADD) extends Module {
  val bw = expW + sigW
  val io = IO(new Bundle {
    val in_a  = Input(Bits(bw.W))
    val in_b  = Input(Bits(bw.W))
    val out = Output(Bits(bw.W))
  })

  override def desiredName = s"FP${mode}_${expW}_$sigW"
  if (mode == FPOPTest.MUL) {
    val opRecFN = Module(new MulRecFN(expW, sigW))
    opRecFN.io.a := recFNFromFN(expW, sigW, io.in_a)
    opRecFN.io.b := recFNFromFN(expW, sigW, io.in_b)
    opRecFN.io.roundingMode := 0.U
    opRecFN.io.detectTininess := 0.U
    // ignore addRecFN.io.exceptionFlags for now
    io.out := fNFromRecFN(expW, sigW, opRecFN.io.out)

  } else if (mode == FPOPTest.ADD || mode == FPOPTest.SUB) {
    val opRecFN = Module(new AddRecFN(expW, sigW))
    opRecFN.io.subOp := false.B
    opRecFN.io.a := recFNFromFN(expW, sigW, io.in_a)
    if (mode == FPOPTest.SUB) {
      opRecFN.io.b := recFNFromFN(expW, sigW, Cat(~io.in_b(bw - 1), io.in_b(bw - 2, 0)))
    } else {
      opRecFN.io.b := recFNFromFN(expW, sigW, io.in_b)
    }
    opRecFN.io.roundingMode := 0.U
    opRecFN.io.detectTininess := 0.U
    // ignore addRecFN.io.exceptionFlags for now
    io.out := fNFromRecFN(expW, sigW, opRecFN.io.out)
  }
}

/*
class FP16Add() extends Module {
  val expW: Int = 8
  val sigW: Int = 24
  val bw = expW + sigW
  val io = IO(new Bundle {
    val in_a = Input(Bits(bw.W))
    val in_b = Input(Bits(bw.W))
    val out = Output(Bits(bw.W))
  })
  val opRecFN = Module (new AddRecFN (expW, sigW) )
  opRecFN.io.subOp := false.B
  opRecFN.io.a := recFNFromFN (expW, sigW, io.in_a)
  opRecFN.io.b := recFNFromFN (expW, sigW, io.in_b)
  opRecFN.io.roundingMode := 0.U
  opRecFN.io.detectTininess := 0.U
  // ignore addRecFN.io.exceptionFlags for now
  io.out := fNFromRecFN (expW, sigW, opRecFN.io.out)
}

class FP16Mul() extends Module {
  val expW: Int = 8
  val sigW: Int = 24
  val bw = expW + sigW
  val io = IO(new Bundle {
    val in_a = Input(Bits(bw.W))
    val in_b = Input(Bits(bw.W))
    val out = Output(Bits(bw.W))
  })
  val opRecFN = Module(new MulRecFN(expW, sigW))
  opRecFN.io.a := recFNFromFN(expW, sigW, io.in_a)
  opRecFN.io.b := recFNFromFN(expW, sigW, io.in_b)
  opRecFN.io.roundingMode := 0.U
  opRecFN.io.detectTininess := 0.U
  // ignore addRecFN.io.exceptionFlags for now
  io.out := fNFromRecFN(expW, sigW, opRecFN.io.out)
}

class FP16PCA(npixels:Int=16) extends Module {
  val expW: Int = 8
  val sigW: Int = 24
  val bw = expW + sigW
  val io = IO(new Bundle {
    val in_a = Input(Vector(npixels, Bits(bw.W)))
    val in_b = Input(Vector(npixels, Bits(bw.W)))
    val out = Output(Bits(bw.W))
  })
}
*/

class FPCompTest(val expW: Int = 8, val sigW: Int = 24) extends Module {
  val bw = expW + sigW
  val io = IO(new Bundle {
    val in_a  = Input(Bits(bw.W))
    val in_b  = Input(Bits(bw.W))
    val out_eq = Output(Bits(1.W))
    val out_lt = Output(Bits(1.W))
    val out_gt = Output(Bits(1.W))
  })

  val compRecFN = Module(new CompareRecFN(expW, sigW))
  compRecFN.io.a := recFNFromFN(expW, sigW, io.in_a)
  compRecFN.io.b := recFNFromFN(expW, sigW, io.in_b)
  compRecFN.io.signaling := true.B // what does this do?
  // ignore addRecFN.io.exceptionFlags for now
  io.out_eq := compRecFN.io.eq
  io.out_lt := compRecFN.io.lt
  io.out_gt := compRecFN.io.gt
}

object FPOPTest {
  trait Mode
  case object ADD extends Mode
  case object SUB extends Mode
  case object MUL extends Mode

  def main(args: Array[String]): Unit = {
    val opts = Array("--disable-all-randomization",
      "--strip-debug-info",
      "--split-verilog",
      "-o=generated")
    ChiselStage.emitSystemVerilog(new FPOPTest(), firtoolOpts = opts)
  }
}
