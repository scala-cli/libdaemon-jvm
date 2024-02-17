package libdaemonjvm.internal

trait LockProcess {
  def pid(): Int
  def isRunning(pid: Int): Boolean
}

object LockProcess {
  class Default extends LockProcess {
    def pid(): Int =
      ProcessHandle.current().pid().toInt
    def isRunning(pid: Int): Boolean =
      ProcessHandle.of(pid).map(p => p.isAlive).orElse(false)
  }

  def default: LockProcess =
    new Default
}
