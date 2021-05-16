package org.clulab.rulemaking.utils

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration

object TimeoutFuture {
  def apply[A](timeout: FiniteDuration)(block: => A)(implicit executor: ExecutionContext): Future[A] =
    try {
      Future {Await.result(Future {block}, timeout)}
    } catch {
      case _: TimeoutException =>
        Future.failed(new TimeoutException(s"Timed out after ${timeout.toString}"))
    }
}

object Execution {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
}