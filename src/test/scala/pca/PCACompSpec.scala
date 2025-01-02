
package pca

import chiseltest._
import scala.util.Random
import common.CommonSpecConfig

class PCACompBlockSpec extends CommonSpecConfig {
  behavior of "PCACompBlock"

  val blockid : Int = 0
  val pxbw: Int = 12
  val width: Int = 8
  val height: Int = 8
  val encsize: Int = 10
  val encbw : Int = 8

  val ninpixels = width * height

  "encmat load" should "pass" in {
    test(new PCACompBlock(blockid, pxbw, width, height, encsize, encbw)) { dut =>
      // set
      println("Loading...")
      dut.io.setencdata.poke(true)
      for(b <- 0 until ninpixels) {
        print(f"$b  ")
        for (i <- 0 until encsize) {
          val v = (b + i) % (1 << (encbw - 1))
          dut.io.encdata(b)(i).poke(v)
        }
      }
      dut.clock.step()
      dut.io.setencdata.poke(false)
      println()

      println("Verifying...")
      dut.io.getencdata.poke(true)
      for(b <- 0 until ninpixels) {
        print(f"$b  ")
        for (i <- 0 until encsize) {
          val v = (b + i) % (1 << (encbw - 1))
          dut.io.encdata(b)(i).poke(v)
          dut.io.encdataverify(b)(i).expect(v)
          // val ret = dut.io.encdataverify(b)(i).peekInt()
          // println(f"[$col:$b:$i] $v should be $ret")
        }
        dut.clock.step()
      }
      println()

      dut.io.getencdata.poke(false)
      println("done")
    }
  }
}
