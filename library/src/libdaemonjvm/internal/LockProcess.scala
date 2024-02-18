package libdaemonjvm.internal

trait LockProcess {
  def pid(): Int
  def isRunning(pid: Int): Boolean
}

object LockProcess {
  class Default extends LockProcess {
    def pid(): Int =
      ProcessHandle.current().pid().toInt
    def isRunning(pid: Int): Boolean = {
      val maybeHandle = ProcessHandle.of(pid)
      if (maybeHandle.isEmpty) false
      else maybeHandle.get.isAlive
    }
  }

  def default: LockProcess =
    new Default
}
