package oldimpl

import chisel3._
import chisel3.simulator.VCDHackedEphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class PCARef(N: Int, M: Int, PXBW: Int, IEMBW: Int, seed: Option[Long] = Some(11L)) {
  private val rng = seed match {
    case Some(seed) => new scala.util.Random(seed)
    case None       => new scala.util.Random()
  }

  val maxUnsignedPXBW = (1 << PXBW) - 1
  val row = Array.fill(N)(rng.nextInt(maxUnsignedPXBW+1))

  // Note: transposed for easier principal component access
  val maxAbsIEMBW = (1 << (IEMBW - 1))
  val matrix = Array.fill(M, N)(rng.between(-maxAbsIEMBW, maxAbsIEMBW))



  def rowVectorMatrixProduct(): Array[Long] = {
    require(row.length == matrix(0).length, "Row-vector length must match number of matrix rows")
    val M = matrix.length
    Array.tabulate(M) { j =>
      (0 until row.length).map(i => row(i).toLong * matrix(j)(i).toLong).sum
    }
  }

  def print(): Unit = {
    val result = rowVectorMatrixProduct()
    println("Row Vector: " + row.mkString("[", ", ", "]"))
    println("Matrix:")
    matrix.foreach(row => println(row.mkString("[", ", ", "]")))
    println("Result: " + result.mkString("[", ", ", "]"))
  }
}

object PCARefTest extends App {
  val N = 8
  val M = 3
  val PXBW  = 8
  val IEMBW = 8

  val pcaref = new PCARef(N, M, PXBW, IEMBW)
  pcaref.print()
}

class BaseLinePCACompSpec extends AnyFlatSpec {
  behavior of "BaseLinePCAComp"

  val W = 2
  val H = W
  val N = W * H
  val M = 3
  val PXBW  = 4
  val IEMBW = 6

  def resetBaseLinePCAComp(dut: BaseLinePCAComp): Unit = {
    // EphemeralSimulator required a reset explicitly for some reason
    dut.reset.poke(true)
    dut.clock.step(1)
    dut.reset.poke(false)
    dut.clock.step(1) // initialize the regs with default value
  }

  def uploadRefBaseLinePCAComp(dut: BaseLinePCAComp, refpca: PCARef, verify: Boolean = false): Unit = {
    dut.io.updateIEM.poke(true)
    for(iempos <- 0 until M) {
      dut.io.iempos.poke(iempos)
      for(pxpos <- 0 until N) {
        val v = refpca.matrix(iempos)(pxpos)
        //println(f"iem${iempos}:px${pxpos} ${v}")
        dut.io.iemdata(pxpos).poke(v.S(IEMBW.W))
      }
      dut.clock.step()
    }
    dut.io.updateIEM.poke(false)

    if(verify) {
      dut.io.verifyIEM.poke(true)
      for (iempos <- 0 until M) {
        dut.io.iempos.poke(iempos)
        dut.clock.step()

        for (pxpos <- 0 until N) {
          val hw = dut.io.iemdataverify(pxpos).peek().litValue.toInt
          val e = refpca.matrix(iempos)(pxpos)
          assert(hw == e, f"hw=$hw ref=$e at iempos${iempos}/pxpos${pxpos}")
        }
      }
      dut.io.verifyIEM.poke(false)
      dut.clock.step(1)
    }
  }

  def computeAndCompare(dut: BaseLinePCAComp, refpca: PCARef) : Unit = {
    dut.io.updateIEM.poke(false)
    dut.io.verifyIEM.poke(false)

    dut.io.in.ready.expect(1)

    dut.io.npc.poke(1) // just single principal component first
    for ((elem, idx) <- refpca.row.zipWithIndex) {
      dut.io.in.bits(idx).poke(elem)
    }
    dut.io.in.valid.poke(true)
    dut.clock.step()

    dut.io.out.ready.poke(true)
    dut.io.out.valid.expect(true.B)
    for ((elem, idx) <- refpca.rowVectorMatrixProduct().zipWithIndex) {
      val v = dut.io.out.bits(idx).peek().litValue.toLong
      assert(v == elem, s"dut=${v} != ref=${elem}")
      dut.clock.step()
    }
    dut.io.out.ready.poke(false)
  }

  "iem update test" should "pass" in {
    simulate(new BaseLinePCAComp(
      pxbw = PXBW, width = W, height = H,
      iemsize = M, iembw = IEMBW,
      debugprint = false)) { dut =>

      val pcaref = new PCARef(N, M, PXBW, IEMBW)
      // pcaref.print()

      uploadRefBaseLinePCAComp(dut, pcaref, verify = true)

      computeAndCompare(dut, pcaref)
    }
  }
}
