package org.clulab.rulemaking

import java.io.File

import ai.lum.common.FileUtils._
import ai.lum.regextools.OdinPatternBuilder
import org.clulab.rulemaking.Consolidator.random

import scala.collection.mutable.ArrayBuffer
import upickle.default.{macroRW, ReadWriter => RW}
import upickle.default._


/** Loads stimuli and converts them to the Traversal */
class Consolidator(nodeVocabulary: File, stimuliFile: File) {

  val nodeNames: Array[String] = loadNodes(nodeVocabulary)
  val nodeColors: Array[String] = mkColors(nodeNames)
  val stimuli: Seq[Stimulus] = loadStimuli(stimuliFile)

  def loadStimuli(file: File): Seq[Stimulus] = {
    val lines = file.readString().split("\n")
    lines.map(line => read[Stimulus](line))
  }

  def mkTraversals(ss: Seq[Stimulus]): Seq[Traversal] = ss.map(mkTraversal)
  def mkTraversal(s: Stimulus): Traversal = {
    val numHops = s.steps.length
    val numNodes = s.node_attributes.length
    require(numHops == numNodes - 1)

    // add the start
    val startNode: Int = s.steps.head._1
    val startConstraint = mkConstraint(s.node_attributes.head, startNode)

    val destinations: Seq[Int] = s.steps.map(tup => tup._2)

    val traversals = new ArrayBuffer[Traversal]()
    traversals.append(startConstraint)

    for ((dst, stepIdx) <- destinations.zipWithIndex) {
      val dstConstraint = mkConstraint(s.node_attributes(stepIdx+1), dst)
      val currTraversal = mkTraversal(s.directions(stepIdx), s.edge_labels(stepIdx), dstConstraint)
      traversals.append(currTraversal)
    }

    Concat(traversals)
  }

  def mkConstraint(attribute: String, n: Int): Constraint = {
    attribute match {
      case NAME => NameConstraint(nodeNames(n))
      case COLOR => ColorConstraint(nodeColors(n))
      case _ => ???
    }
  }

  def mkTraversal(direction: String, edgeLabel: String, dst: Constraint): Traversal = {
    direction match {
      case INCOMING => IncomingEdge(edgeLabel, dst)
      case OUTGOING => OutgoingEdge(edgeLabel, dst)
      case _ => ???
    }
  }

  def addQuantifiers(traversal: Traversal): Traversal = {
    // is a leaf
    if (traversal.isTerminal) return maybeWrap(traversal)

    // has children -- wrap them
    val children = traversal.children
    val quantifiedChildren = children.map(addQuantifiers)

    if (quantifiedChildren.length < 3) {
      return traversal.copyWithChildren(Seq(quantifiedChildren.head))
    }

    // otherwise, it's a Concat or Or and > 1 child
    require(quantifiedChildren.length >= 3)
    val maybeSlice = pickSliceWithPatience(patience = 5, quantifiedChildren)
    if (maybeSlice.isEmpty) return traversal.copyWithChildren(quantifiedChildren)

    // otherwise, keep going bc we found a slice
    val (sliceStart, sliceEnd) = maybeSlice.get

    val out = new ArrayBuffer[Traversal]()
    var i = 0
    while (i < quantifiedChildren.length) {
      val c = quantifiedChildren(i)
      i match {
        case x if x < sliceStart =>
          out.append(c)
          i += 1
        case x if x >= sliceEnd =>
          out.append(c)
          i += 1
        case x if x == sliceStart =>
          val sliceTraversal: Traversal = traversal.copyWithChildren(quantifiedChildren.slice(sliceStart, sliceEnd))
          val newTraversal: Traversal = maybeWrap(sliceTraversal)
          out.append(newTraversal)
          i = sliceEnd
      }
    }

    traversal.copyWithChildren(out)
  }

  def pickSliceWithPatience(patience: Int, traversals: Seq[Traversal]): Option[(Int, Int)] = {
    for (_ <- 0 until patience) {
      val maybeSlice = pickSlice(traversals)
      if (maybeSlice.isDefined) {
        return maybeSlice
      }
    }
    None
  }

  def pickSlice(traversals: Seq[Traversal]): Option[(Int, Int)] = {
    val sliceStart = pickValidStart(traversals)
    if (sliceStart < 0) return None
    val sliceEnd = pickValidEnd(traversals, sliceStart + 2) // slice must have at least 2 traversals
    if (traversals.slice(sliceStart, sliceEnd).exists(t => !t.isInstanceOf[Constraint])) {
      Some((sliceStart, sliceEnd))
    } else {
      None
    }
  }


  def pickValidStart(traversals: Seq[Traversal]): Int = {
    val available = traversals.indices
      .slice(0, traversals.length - 2)// subtract 2 so you can have a valid slice of at least 2 elements
      .filterNot(traversals(_).isInstanceOf[Constraint]) // can't start with a constraint
    if (available.nonEmpty){
      val selection = random.nextInt(available.length)
      available(selection)
    } else -1
  }

  // next possible is two after the start
  def pickValidEnd(traversals: Seq[Traversal], nextPossible: Int): Int = {
    (nextPossible + random.nextInt(traversals.length - nextPossible))
  }

  def maybeWrap(traversal: Traversal): Traversal = {
    if (traversal.isInstanceOf[Constraint]) return traversal

    // flip a coin
    val flip = random.nextInt(7)
    flip match {
      case x if x < 4 => traversal // do nothing
      case 4 => KleeneStar(traversal)
      case 5 => KleenePlus(traversal)
      case 6 => Optional(traversal)
      case _ => ???
    }
  }

  def mkPerturbations(traversal: Traversal, n: Int): Seq[Traversal] = {
    for {
      _ <- 0 until n
    } yield addQuantifiers(traversal)
  }

  def mkPerturbations(traversals: Seq[Traversal], n: Int): Seq[Traversal] = {
    traversals.flatMap(mkPerturbations(_, n))
  }


  // TODO: add quantifiers
  // ? + * ranges
  // Quantifiers: insert after an atomic thing OR after a sequence of (step, node)
  // a* b c --- a* b* c* --- (abc)*
  // nesting -- do through recursive -- control with random max depth
  // (x (a b c)* y){3,5}

  // TODO: add OR

  // are these patterns equivalent?

  // TODO: future:
  //  automatically evaluating what "correct" and "incorrect" answers are
  //  automatically evaluating the values of all factors (independent measures and controls)


}

object Consolidator {
  val random = scala.util.Random
  val patternBuilder = new OdinPatternBuilder
}

// {"steps": [[2, 0], [0, 2], [2, 6], [6, 5], [5, 6], [6, 1], [1, 0], [0, 2], [2, 0]],
// "directions": ["OUT", "OUT", "OUT", "IN", "OUT", "OUT", "IN", "IN", "IN"],
// "edge_labels": ["F", "D", "E", "C", "C", "F", "H", "C", "D"],
// "node_attributes": ["NAME", "NAME", "NAME", "COLOR", "COLOR", "NAME", "COLOR", "NAME", "COLOR", "NAME"]}
case class Stimulus(steps: Seq[(Int, Int)], directions: Seq[String], edge_labels: Seq[String], node_attributes: Seq[String])
object Stimulus {
  implicit val rw: RW[Stimulus] = macroRW
}


