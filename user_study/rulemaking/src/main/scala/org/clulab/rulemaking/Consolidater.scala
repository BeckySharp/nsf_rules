package org.clulab.rulemaking

case class Stimulus()
case class Pattern()

object Consolidater extends App {

  def loadStimuli(fn: String): Seq[Stimulus] = {
    ???
  }

  def consolidate(ss: Seq[Stimulus]): Seq[Pattern] = ss.map(consolidate)
  def consolidate(s: Stimulus): Pattern = ???
}

