package pca

import chisel3._
import chisel3.util._
import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._
//import chisel3.simulator.ChiselSim  // for 7.0 or later

/**
 * PCA testdata generation
 *
 * vec : row-vector with the size n. vecbw-bit unsigned integer
 * mat : matrix with n rows and m cols. matbw-bit signed integer
 *
 * @param nvecs the number of the input vectors
 * @param n the number of the rows in mat and the length of vec
 * @param m the number of the columns in vec
 */
class PCATestData(val nvecs: Int, val n: Int, val m: Int, vecbw: Int, matbw: Int) {
  val resbw = vecbw + matbw + log2Ceil(n)
  require(resbw < 64)

  val rnd = new Random(123)


  val mat: Array[Array[Long]] = Array.fill(n, m) {
    val tmp = 1 << matbw
    rnd.between(-tmp, tmp)
  }

  val inpvecs: Array[Array[Long]] = Array.fill(nvecs, n) {
    rnd.nextInt(1 << vecbw)
  }

  val ref: Array[Long] = Array.fill(m)(0.toLong)
  for (inidx <- 0 until nvecs) {
    for (encidx <- 0 until m) {
      ref(encidx) = ref(encidx) +
        (0 until n).map(j => inpvecs(inidx)(j) * mat(j)(encidx)).sum
    }
  }
}

class PCACompBlockSpec extends AnyFlatSpec {
  behavior of "PCACompBlock"

  "PCA basic test" should "pass" in {
    simulate(new PCACompBlock(nrows = 4, nmaxpcs = 10)) { dut =>
      val pcadata = new PCATestData(dut.nrows, dut.width, dut.nmaxpcs, dut.pxbw, dut.iembw)
      println(pcadata.ref.mkString(" "))
    }
  }
}
