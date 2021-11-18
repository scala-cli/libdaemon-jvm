package libdaemonjvm.server

import java.nio.file.Path

sealed abstract class LockError(
  message: String,
  cause: Throwable = null
) extends Exception(message, cause)

object LockError {
  final class AlreadyRunning(val pid: Int)
      extends LockError(s"Daemon already running (PID: $pid)")
  final class CannotDeleteFile(val file: Path, cause: Throwable)
      extends LockError(s"Cannot delete $file", cause)
  final class ZombieFound(val pid: Int, val connectionError: Throwable)
      extends LockError(s"Cannot connect to process $pid", connectionError)
}
