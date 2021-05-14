package org.clulab

import java.io.File
import ai.lum.common.FileUtils._

package object rulemaking {

  val DISPLAY_FIELD: String = "raw"
  val EDGE_FIELD: String = "edges"
  val NODE_COLOR_FIELD: String = "color"
  val NODE_NAME_FIELD: String = "node"
  val NAME: String = "NAME"
  val COLOR: String = "COLOR"
  val INCOMING: String = "IN"
  val OUTGOING: String = "OUT"
  //---------------------------
  val NODE: String = "TOKEN"
  val EDGE: String = "EDGE"

  def loadNodes(file: File): Array[String] = {
    file.readString().split("\n").map(_.trim)
  }

  def mkColors(nodeNames: Array[String]): Array[String] = {
    val out = new Array[String](nodeNames.length)
    for (i <- nodeNames.indices) {
      var color = "white"
      if (i % 3 == 0) color = "blue"
      if (i % 4 == 0) color = "orange"
      if (i % 5 == 0) color = "gray"
      out(i) = color
    }
    out
  }

}
