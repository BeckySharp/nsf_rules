package org.clulab.rulemaking

import java.io.File

import ai.lum.common.FileUtils._
import ai.lum.odinson
import ai.lum.odinson.{Document, ExtractorEngine, GraphField, Mention, Sentence}
import com.typesafe.config.Config
import upickle.default.{macroRW, ReadWriter => RW}
import upickle.default._

/** Verifies whether or not a pattern matches the current graph, as well
 * as what it matches in that graph. */
class Verifier(config: Config, nodeVocabulary: File, packageDir: File) {
  val nodeNames: Array[String] = loadNodes(nodeVocabulary)
  val nodeColors: Array[String] = mkColors(nodeNames)
  val graph: Document = loadGraph(packageDir)
  // index the document for the EE
  val ee: ExtractorEngine = ExtractorEngine.inMemory(config, Seq(graph))
  val availableNodes: Array[String] = ee.getTokensForSpan(0, 0, graph.sentences.head.numTokens)

  // verify that a pattern matches
  def isVerified(pattern: String): Boolean = {
    val found = query(pattern)
    found.nonEmpty
  }

  def query(pattern: String): Array[Mention] = {
    val rules =
      s"""
        |rules:
        | - name: stimuli_rule
        |   type: basic
        |   label: Result
        |   pattern: |
        |     ${pattern}
        |""".stripMargin
    ee.extractMentions(ee.compileRuleString(rules)).toArray
  }

  def getNames(ms: Array[Mention]): Array[String] = {
    ms.map(m => ee.getTokensForSpan(m).mkString(" ")).distinct
  }

  // does the pattern match other things?
//  def otherMatches(pattern: String, expected: String): Seq[Mention] = {
//    val found = query(pattern)
//    found.filterNot(mention => ee.getStringForSpan(mention.luceneDocId, mention.odinsonMatch) == expected)
//  }

  // load the full_graph --> convert to Document
  def loadGraph(packageDir: File): Document = {
    val graphDir = packageDir.mkChild("graph")
    val graphFiles = graphDir.listFilesByWildcard("*.json")
    val subgraphs = graphFiles.map(gf => read[SubgraphEdges](gf.readString()))
    val graphField = allEdgesToGraphField(subgraphs.toVector)
//    val nodeIds = subgraphs.flatMap(_.allNodes).toVector.distinct
    val rawField = odinson.TokensField(DISPLAY_FIELD, nodeNames)
    val namesField = odinson.TokensField(NODE_NAME_FIELD, nodeNames)
    val colorsField = odinson.TokensField(NODE_COLOR_FIELD, nodeColors)
    val sentence = Sentence(nodeNames.length, Seq(rawField, namesField, colorsField, graphField))
    Document(packageDir.getBaseName(), Seq.empty, Seq(sentence))
  }

  case class SubgraphEdges(edges: Seq[(Int, Int)], label: String) {
    val labeled = edges.map{case (src, dst) => (src, dst, label)}
    val root = edges.head._1
    val allNodes = edges.flatMap(tup => Vector(tup._1, tup._2)).toSet
  }
  object SubgraphEdges {
    implicit val rw: RW[SubgraphEdges] = macroRW
  }

  def allEdgesToGraphField(subgraphs: Seq[SubgraphEdges]): GraphField = {
    val edges = subgraphs.flatMap(_.labeled)
    val roots = subgraphs.map(_.root).toSet
    odinson.GraphField(EDGE_FIELD, edges, roots)
  }



}
