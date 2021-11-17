package libdaemonjvm.internal

import scala.util.Properties
import scala.sys.process._

object Processes {
  def isRunning(pid: Int): Boolean = {
    val pids =
      if (Properties.isWin) {
        val output = Seq(System.getenv("WINDIR") + "\\system32\\tasklist.exe", "/fo", "list").!!
        output
          .linesIterator
          .map(_.trim)
          .filter(_.startsWith("PID:"))
          .map(_.stripPrefix("PID:").trim)
          .map(_.toInt)
          .toSet
      }
      else {
        val output = Seq("ps", "-e").!!
        output
          .linesIterator
          .map(_.dropWhile(_.isSpaceChar))
          .filter(_.nonEmpty)
          .filter(_.head.isDigit)
          .map(_.takeWhile(_.isDigit))
          .map(_.toInt)
          .toSet
      }
    pids.contains(pid)
  }
}
