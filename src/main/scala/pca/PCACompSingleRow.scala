package pca

import chisel3._
import chisel3.util._
import common.GenVerilog

class PCACompSingleRow(
                        // pixel-sensor params. the width and height of a block
                        ncols: Int = 192, // the numbers of the pixel-sensor columns
                        nrows: Int = 168, // the numbers of the pixel-sensor rows
                        pxbw: Int = 12,   // pixel bit width
                        width: Int = 24,  // width for this block. ncols%widht == 0
                        // PCA params
                        npcs  : Int = 30, // the max number of principal components
                        iemfloat : Boolean = false, // false: signed integer
                        iembw    : Int = 8, // encoding bit width for int, mantissa bit for float
                        iemexp   : Int = 0, // exponent bit for float
                        // other params
                        debugprint: Boolean = true
                      ) extends Module {

  require( (ncols%width)==0 )

  private val mulbw = pxbw + iembw    // internal use. signed
  val redbw = if (iemfloat) {
    mulbw + log2Ceil(width) // reduction result
  } else {
    1 + iemexp + iembw
  }

  override def desiredName = s"BaseLinePCAComp_pxbw${pxbw}_w${width}_iembw${iembw}_npcs${npcs}"

  val io = IO(new Bundle {
    // input : no stall condition for now
    val npc     = Input(UInt(log2Ceil(npcs).W)) // the number of principal components used
    val rowid   = Input(UInt(log2Ceil(nrows).W))
    val invalid = Input(Bool())
    val indata  = Input(Vec(width, UInt(pxbw.W)))
    // output (buffered)
    val out  = Decoupled(Vec(npcs, SInt(redbw.W))) // compressed data

    // initialize memory with the inverse encoding matrix content for this block
    val updateIEM     = Input(Bool()) // load imedata into mem
    val verifyIEM     = Input(Bool()) // read mem for verification
    val rowpos        = Input(UInt(log2Ceil(nrows).W))
    val iempos        = Input(UInt(log2Ceil(npcs).W))
    val iemdata       = Input(Vec(width, SInt(iembw.W)))
    val iemdataverify = Output(Vec(width, SInt(iembw.W)))
  })

  io.out.valid := false.B
  io.out.bits := 0.U.asTypeOf(Vec(npcs, SInt(redbw.W)))
  io.iemdataverify := 0.U.asTypeOf(Vec(width, SInt(iembw.W)))
}

object PCACompSingleRow extends App {
  GenVerilog(new PCACompSingleRow)
}
