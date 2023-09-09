package pca

//import chisel3.stage.ChiselStage
import _root_.circt.stage.ChiselStage // for 5.0.0

object GenPCA {
  def main(args: Array[String]): Unit = {
    // option handling
    val n=256
    val nbits_px=8
    val nbits_iem=8
    val opts = Array("--disable-all-randomization",
       "--strip-debug-info",
      //   "--split-verilog", "-o=generated",
      "--lowering-options=disallowLocalVariables",
      "--verilog", s"-o=VMulRed_n${n}_px${nbits_px}_iem${nbits_iem}.v"
    )

    ChiselStage.emitSystemVerilog(new VMulRed(n,nbits_px,nbits_iem), firtoolOpts = opts) // 5.0.0
  }
}
