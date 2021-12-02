package libdaemonjvm.server

import java.nio.file.Path

sealed abstract class LockError(
  message: String,
  cause: Throwable = null
) extends Exception(message, cause)

object LockError {

  sealed abstract class RecoverableError(
    message: String,
    cause: Throwable = null
  ) extends LockError(message, cause)

  sealed abstract class FatalError(
    message: String,
    cause: Throwable = null
  ) extends LockError(message, cause)

  final class AlreadyRunning(val pid: Int)
      extends FatalError(s"Daemon already running (PID: $pid)")
  final class CannotDeleteFile(val file: Path, cause: Throwable)
      extends FatalError(s"Cannot delete $file", cause)
  final class ZombieFound(val pid: Int, val connectionError: Throwable)
      extends RecoverableError(s"Cannot connect to process $pid", connectionError)
}
