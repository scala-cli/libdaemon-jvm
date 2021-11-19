package libdaemonjvm.client

sealed abstract class ConnectError(
  message: String,
  cause: Throwable = null
) extends Exception(message, cause)

object ConnectError {
  final class ZombieFound(val pid: Int, val connectionError: Throwable)
      extends ConnectError(s"Cannot connect to process $pid", connectionError)
}
