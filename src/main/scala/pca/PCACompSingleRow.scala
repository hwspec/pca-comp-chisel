package pca

import chisel3._
import chisel3.util._
import common.GenVerilog

class PCACompSingleRow(
                        // pixel-sensor params. the width and height of a block
                        ncols: Int = 192, // the numbers of the pixel-sensor columns
                        nrows: Int = 168, // the numbers of the pixel-sensor rows
                        pxbw: Int = 12, // pixel bit width
                        width: Int = 32, // width for this block. ncols%widht == 0
                        // PCA params, iem=inverse encoding matrix
                        nmaxpcs  : Int = 60, // the max number of principal components
                        iemfloat : Boolean = false, // false: signed integer
                        iembw    : Int = 8, // encoding bit width for int, mantissa bit for float
                        iemexp   : Int = 0, // exponent bit for float. unused for signed integer
                        // other params
                        debugprint: Boolean = true
                      ) extends Module {

  require((ncols % width) == 0)
  require(iemfloat == false, "float is not supported yet")

  private val mulbw = pxbw + iembw // internal use. signed
  val redbw = if (iemfloat) {
    mulbw + log2Ceil(width) + log2Ceil(nrows) // reduction result
  } else {
    1 + iemexp + iembw
  }

  val databw = if (iemfloat) {
    1 + iemexp + iembw
  } else iembw
  val busbw = width * databw

  override def desiredName = s"BaseLinePCAComp_pxbw${pxbw}_w${width}_iembw${iembw}_npcs${nmaxpcs}"

  val io = IO(new Bundle {
    // input : no stall condition for now
    // val npc = Input(UInt(log2Ceil(nmaxpcs).W)) // the number of principal components used
    val rowid = Input(UInt(log2Ceil(nrows).W))
    val invalid = Input(Bool())
    val indata = Input(Vec(width, UInt(pxbw.W)))
    // output
    val out = Output(Vec(nmaxpcs, SInt(redbw.W)))

    // initialize memory with the inverse encoding matrix content for this block
    val updateIEM = Input(Bool()) // load imedata into mem
    val verifyIEM = Input(Bool()) // read mem for verification
    val rowpos = Input(UInt(log2Ceil(nrows).W))
    val iempos = Input(UInt(log2Ceil(nmaxpcs).W))
    val iemdata = Input(Vec(width, SInt(iembw.W)))
    val iemdataverify = Output(Vec(width, SInt(iembw.W)))
  })

  io.iemdataverify := 0.U.asTypeOf(Vec(width, SInt(iembw.W)))

  val mems = Seq.fill(nmaxpcs)(SyncReadMem(nrows, UInt(busbw.W)))

  val dataReceived = RegInit(false.B)

  when(io.updateIEM) {
    for(i <- 0 until nmaxpcs) {
      when(io.iempos === i.U) {
        mems(i).write(io.rowpos, io.iemdata.asTypeOf(UInt(busbw.W)))
      }
    }
  }.elsewhen(io.verifyIEM) {
    for(i <- 0 until nmaxpcs) {
      when(io.iempos === i.U) {
        io.iemdataverify := mems(i).read(io.rowpos, true.B).asTypeOf(Vec(width, SInt(iembw.W))) // available in cycle later
      }
    }
  }.otherwise {
    when(io.invalid) {
      dataReceived := true.B
    }.otherwise {
      dataReceived := false.B
    }
  }

  val fromiem = Wire(Vec(nmaxpcs, Vec(width, SInt(iembw.W))))
  val multiplied = Wire(Vec(nmaxpcs, Vec(width, SInt((pxbw+iembw).W))))
  val partialcompressed = Wire(Vec(nmaxpcs,SInt(redbw.W)))
  val compressedAccReg = RegInit(VecInit(Seq.fill(nmaxpcs)(0.S(redbw.W))))
  val compressedReg = RegInit(VecInit(Seq.fill(nmaxpcs)(0.S(redbw.W))))

  for(pos <- 0 until nmaxpcs) {
    fromiem(pos) := mems(pos).read(io.rowid, true.B).asTypeOf(Vec(width, SInt(iembw.W)))
    when(dataReceived) {
      for (x <- 0 until width) {
        multiplied(pos)(x) := fromiem(pos)(x) * io.indata(x)
      }
      partialcompressed(pos) := multiplied(pos).reduce(_ +& _)
    }.otherwise {
      for (x <- 0 until width) {
        multiplied(pos)(x) := 0.S
      }
      partialcompressed(pos) := 0.S
    }
    compressedAccReg(pos) := compressedAccReg(pos) + partialcompressed(pos)
    when(io.rowid === (nrows-1).U) {
      compressedReg(pos) := compressedAccReg(pos)
    }
  }

  io.out := compressedReg
}

object PCACompSingleRow extends App {
  GenVerilog(new PCACompSingleRow)
}
