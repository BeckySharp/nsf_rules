package org.clulab.rulemaking

trait Traversal {
  def children: Seq[Traversal] = Seq.empty
  def asMaybeWild: Traversal = ???
  def isTerminal: Boolean = true
  def copyWithChildren(newChildren: Seq[Traversal]): Traversal = ???
}

case class OutgoingEdge(name: String, dst: Constraint = NoConstraint()) extends Traversal {
  override def toString: String = s">$name $dst"
  override def asMaybeWild: Traversal = OutgoingWild(dst)
  def copy(n: String = name, d: Constraint = dst): Traversal = OutgoingEdge(n, d)
}

case class IncomingEdge(name: String, dst: Constraint = NoConstraint()) extends Traversal{
  override def toString: String = s"<$name $dst"
  override def asMaybeWild: Traversal = IncomingWild(dst)
  def copy(n: String, d: Constraint = dst): Traversal = IncomingEdge(n, d)
}

case class OutgoingWild(dst: Constraint = NoConstraint()) extends Traversal {
  override def toString: String = s">> $dst"
  override def asMaybeWild: Traversal = this
//  def copy(d: Constraint = dst): Traversal = OutgoingWild(d)
}

case class IncomingWild(dst: Constraint = NoConstraint()) extends Traversal{
  override def toString: String = s"<< $dst"
  override def asMaybeWild: Traversal = this
//  def copy(d: Constraint = dst): Traversal = IncomingWild(d)
}

case class Concat(traversals: Seq[Traversal]) extends Traversal {
  override def toString: String = traversals.map(_.toString).mkString(" ") // todo smarter usage of parens?
  override def children: Seq[Traversal] = traversals
  override def isTerminal: Boolean = false
  override def asMaybeWild: Traversal = this
  override def copyWithChildren(newChildren: Seq[Traversal]): Traversal = this.copy(newChildren)
}

case class Or(traversals: Seq[Traversal]) extends Traversal {
  override def toString: String = s"(${traversals.map(_.toString).mkString(" | ")})" // todo smarter usage of parens?
  override def children: Seq[Traversal] = traversals
  override def isTerminal: Boolean = false
  override def asMaybeWild: Traversal = this
  override def copyWithChildren(newChildren: Seq[Traversal]): Traversal = this.copy(newChildren)
}

case class KleeneStar(traversal: Traversal) extends Traversal {
  override def toString: String = s"(${traversal.toString})*"
  override def children: Seq[Traversal] = Seq(traversal)
  override def isTerminal: Boolean = false
  override def asMaybeWild: Traversal = this
  override def copyWithChildren(newChildren: Seq[Traversal]): Traversal = {
    require(newChildren.length == 1)
    this.copy(newChildren.head)
  }
}

case class KleenePlus(traversal: Traversal) extends Traversal {
  override def toString: String = s"(${traversal.toString})+"
  override def children: Seq[Traversal] = Seq(traversal)
  override def isTerminal: Boolean = false
  override def asMaybeWild: Traversal = this
  override def copyWithChildren(newChildren: Seq[Traversal]): Traversal = {
    require(newChildren.length == 1)
    this.copy(newChildren.head)
  }
}

case class Optional(traversal: Traversal) extends Traversal {
  override def toString: String = s"(${traversal.toString})?"
  override def children: Seq[Traversal] = Seq(traversal)
  override def isTerminal: Boolean = false
  override def asMaybeWild: Traversal = this
  override def copyWithChildren(newChildren: Seq[Traversal]): Traversal = {
    require(newChildren.length == 1)
    this.copy(newChildren.head)
  }
}

//case class Range(traversal: Traversal, min: Int, max: Int) extends Traversal {
//  override def toString: String = s"(${traversal.toString}){$min, $max}"
//  override def children: Seq[Traversal] = Seq(traversal)
//  override def isTerminal: Boolean = false
//  override def asMaybeWild: Traversal = this
////  def copy(t: Traversal = traversal, mn: Int = min, mx: Int = max): Traversal = Range(t, mn, mx)
//}

//Concat(KleeneStar(patt), xx, yy, Or(...))


trait Constraint extends Traversal {}

case class Wildcard() extends Constraint {
  override def toString: String = "[]"
  override def asMaybeWild: Traversal = this
}

case class NameConstraint(name: String) extends Constraint {
  override def toString: String = s"[node=$name]"
  override def asMaybeWild: Traversal = Wildcard()
}

case class ColorConstraint(color: String) extends Constraint {
  override def toString: String = s"[color=$color]"
  override def asMaybeWild: Traversal = Wildcard()
}

case class NoConstraint() extends Constraint {
  override def toString: String = "[]"
  override def asMaybeWild: Traversal = Wildcard()
}