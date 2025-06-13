package pca

import chisel3._
//import chisel3.simulator.ChiselSim
import scala.math._
import scala.util.Random
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PCACompBlockSpec extends AnyFlatSpec {
  behavior of "PCACompBlock"

  simulate(new PCACompBlock()) { dut =>

  }
}
