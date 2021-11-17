package libdaemonjvm

import libdaemonjvm.internal.{Pid, Processes}

trait LockProcess {
  def pid(): Int
  def isRunning(pid: Int): Boolean
}

object LockProcess {
  class Default extends LockProcess {
    def pid(): Int =
      Option((new Pid).get()).map(n => (n: Int)).getOrElse {
        sys.error("Cannot get PID")
      }
    def isRunning(pid: Int): Boolean =
      Processes.isRunning(pid)
  }

  def default: LockProcess =
    new Default
}
