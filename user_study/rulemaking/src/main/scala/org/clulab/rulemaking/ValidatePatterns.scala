package org.clulab.rulemaking

import ai.lum.common.ConfigUtils._
import java.io.File

import ai.lum.common.ConfigFactory

object ValidatePatterns extends App {
  val config = ConfigFactory.load()

  val packageDir: File = config[File]("packageDir")
  val nodeVocab: File = config[File]("nodeVocab")
  val stimuliFile = new File(packageDir, "stimuli.jsonl")
  val verifier = new Verifier(config, nodeVocab, packageDir)
  val consolidator = new Consolidator(nodeVocab, stimuliFile)

  for (s <- consolidator.stimuli) {
    println(s"stimulus: $s")
    val initial = consolidator.mkTraversal(s)
    val pattern = initial.toString
    println(s"initial pattern: $pattern")
    println(s"Verified? ==> ${verifier.isVerified(pattern)}")
//    val allFound = verifier.otherMatches(pattern, "")
    println(s"num Matches found: ${verifier.query(pattern).length}")
    val quantified = consolidator.addQuantifiers(initial).toString
    println(s"quantified Pattern: ${quantified}")
    println(s"num Matches found with Quantified: ${verifier.query(quantified).length}")
    println()
  }
//  val unrolled = consolidator.mkTraversal(stimuli)
//  val merged = UnrolledPattern.merge(unrolled.slice(3,6))
//  println(s"mergedPattern: $merged")




}
