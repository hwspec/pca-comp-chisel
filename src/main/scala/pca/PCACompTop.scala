// SPDX-License-Identifier: BSD-3-Clause
// Copyright (c) 2025, UChicago Argonne, LLC.
// Main author: Kazutomo Yoshii <kazutomo@anl.gov>. See LICENSE in project root.

package pca

import chisel3._
import common.GenVerilog

class PCACompTop(
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
                  debugprint: Boolean = true) extends Module {


}

object PCACompTop extends App {
  GenVerilog(new PCACompTop)
}
