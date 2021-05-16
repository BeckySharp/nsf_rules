package org.clulab.rulemaking

import ai.lum.common.ConfigUtils._
import ai.lum.common.FileUtils._
import java.io.File

import scala.concurrent.duration._
import ai.lum.common.ConfigFactory
import org.clulab.rulemaking.utils.TimeoutFuture
import org.clulab.rulemaking.utils.Execution._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await

object ValidatePatterns extends App {
  val config = ConfigFactory.load()

  val duration = FiniteDuration(5, SECONDS)

  val packageDir: File = config[File]("packageDir")
  val nodeVocab: File = config[File]("nodeVocab")
  val stimuliFile = new File(packageDir, "stimuli.jsonl")
  val verifier = new Verifier(config, nodeVocab, packageDir)
  val consolidator = new Consolidator(nodeVocab, stimuliFile)

  val outputFile: File = config[File]("outputTsv")

  for (s <- consolidator.stimuli) {
    val row = new ArrayBuffer[String]

    // All the nodes that are available in the graph
    val allNames = verifier.availableNodes
    row.append(allNames.mkString(", "))

    // The initial pattern that we made with our random walk
    val initial = consolidator.mkTraversal(s)
    val pattern = initial.toString
    val initialResults = verifier.query(pattern)
    val matchedNodes = verifier.getNames(initialResults)
    row.append(pattern)
    row.append(initialResults.length.toString)
    row.append(matchedNodes.mkString(", "))

    // The randomly quantified version of that initial random walk
    val quantified = consolidator.addQuantifiers(initial).toString
    val resultsFromQuantified = TimeoutFuture(duration)(verifier.query(quantified))
    val result = Await.result(resultsFromQuantified, Duration.Inf)
    val quantifiedNames = verifier.getNames(result)
    row.append(quantified)
    row.append(result.length.toString)
    row.append(quantifiedNames.mkString(", "))
    println("finished a quantification")

    // export
    outputFile.writeString(row.mkString("\t") + "\n", append=true)

  }




}
