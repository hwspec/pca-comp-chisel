package pca

import chisel3._
import chisel3.util._
import common.GenVerilog

// Baseline works only for small-sized pixel arrays and assumes no shift strategy.
// This is intended for conceptual implementation only.
// It uses registers, instead of SRAM, to store the inversed encoding matrix
// No pipelining

class BaseLinePCAComp(
                       pxbw: Int = 10, width: Int = 16, height: Int = 16,
                       iemsize: Int = 50, iembw: Int = 8, // iem: inversed encoding matrix
                       debugprint: Boolean = true
                     ) extends Module {

  val ninpixels = (width * height)
  val mulbw = pxbw + iembw  // internal use. signed
  val redbw = mulbw + log2Ceil(ninpixels) // internal use. reduction result
  val outbw = redbw - 1 // -1 due to zeroing out

  override def desiredName = s"BaseLinePCAComp_${width}x${height}_bw${pxbw}_iembw${iembw}_sz${iemsize}"

  val io = IO(new Bundle {
    val npc = Input(UInt(log2Ceil(iemsize).W)) // the number of principal components
    val in  = Flipped(Decoupled(Vec(ninpixels, UInt(pxbw.W)))) // row measure
    val out = Decoupled(Vec(iemsize, UInt(outbw.W))) // compressed data
    // note: the sign bit may not be needed, negative values to zero?

    // setup the inversed encoding matrix
    val updateIEM     = Input(Bool()) // load imedata into mem
    val verifyIEM       = Input(Bool()) // read mem for verification
    val iempos        = Input(UInt(log2Ceil(iemsize).W))
    val iemdata       = Input(Vec(ninpixels, SInt(iembw.W)))
    val iemdataverify = Output(Vec(ninpixels, SInt(iembw.W)))
  })

  // val iemmats = Seq.fill(iemsize)(SyncReadMem(ninpixels, SInt(iembw.W)))
  val iemmats = VecInit(Seq.fill(iemsize)(
    RegInit(VecInit(Seq.fill(ninpixels)(0.S(iembw.W))))
  ))

  io.iemdataverify.foreach { e => e := 0.S }
  io.out.bits.foreach { e => e := 0.U}
  io.out.valid := false.B

  when(io.updateIEM) {
    for(memid <- 0 until iemsize) {
      when(io.iempos === memid.U) {
        for(i <- 0 until ninpixels) {
          iemmats(memid)(i) := io.iemdata(i)
        }
      }
    }
  }.elsewhen(io.verifyIEM) {
    for(memid <- 0 until iemsize) {
      when(io.iempos === memid.U) {
        for(i <- 0 until ninpixels) {
          io.iemdataverify(i) := iemmats(memid)(i)
        }
      }
    }
  }

  // compute a row-vector matrix product
  val inProcessing = RegInit(false.B)
  val processingPos = RegInit(0.U(log2Ceil(iemsize).W)) //

  val inpixelsReg = RegInit(VecInit(Seq.fill(ninpixels)(0.U(pxbw.W))))
  val compdataReg = RegInit(VecInit(Seq.fill(iemsize)(0.S(redbw.W))))
  val red = Module(new LocalRedBuiltIn(n=ninpixels, inbw=mulbw))

  val multiplied = Wire(Vec(ninpixels, SInt(mulbw.W)))
  multiplied.foreach { e => e := 0.S }

  red.io.in := multiplied
  compdataReg(processingPos) := red.io.out

  io.in.ready := !inProcessing

  when(io.in.valid && !inProcessing) {
    // initialize
    processingPos := 0.U
    for(i <- 0 until iemsize) { compdataReg(i) := 0.S }

    inpixelsReg := io.in.bits

    // the first principal component
    for(i <- 0 until ninpixels) {
      multiplied(i) := io.in.bits(i) * iemmats(processingPos)(i)
    }

    inProcessing := true.B

    processingPos := processingPos + 1.U
  }

  when(inProcessing) {
    when(processingPos === io.npc) {
      inProcessing := false.B
    }

    // the second principal component or later
    for(i <- 0 until ninpixels) {
      multiplied(i) := inpixelsReg(i) * iemmats(processingPos)(i)
    }
    processingPos := processingPos + 1.U
  }
}

object BaseLinePCAComp extends App {

  Seq(16).foreach { len =>
    GenVerilog.generate(new BaseLinePCAComp(
      pxbw = 10, width = len, height = len,
      iemsize = 50, iembw = 8,
      debugprint = true
    ))
  }
}
