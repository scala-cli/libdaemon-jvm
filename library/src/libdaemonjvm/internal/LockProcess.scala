package libdaemonjvm.internal

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
      Option((new IsRunning).isRunning(pid)).map(b => (b: Boolean)).getOrElse {
        Processes.isRunning(pid)
      }
  }

  def default: LockProcess =
    new Default
}
