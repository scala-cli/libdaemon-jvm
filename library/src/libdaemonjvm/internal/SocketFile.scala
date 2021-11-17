package libdaemonjvm.internal

import java.net.SocketException
import java.nio.channels.SocketChannel
import java.nio.file.Path
import java.net.Socket

import libdaemonjvm.SocketPaths

object SocketFile {
  def canConnect(paths: SocketPaths): Either[Throwable, Unit] = {
    var s: Either[Socket, SocketChannel] = null
    try {
      s = SocketHandler.client(paths)
      Right(())
    }
    catch {
      case e: SocketException =>
        Left(e)
      case e: SocketExceptionLike =>
        Left(e)
    }
    finally if (s != null)
      s.merge.close()
  }
}
