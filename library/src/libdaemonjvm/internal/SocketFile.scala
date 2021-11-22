package libdaemonjvm.internal

import java.net.SocketException
import java.nio.channels.SocketChannel
import java.nio.file.Path
import java.net.Socket

import libdaemonjvm.errors._
import libdaemonjvm.SocketPaths

object SocketFile {
  def canConnect(paths: SocketPaths): Either[Throwable, Unit] = {
    var s: Either[Throwable, Either[Socket, SocketChannel]] = null
    try {
      s = connect(paths)
      s.map(_ => ())
    }
    finally if (s != null)
      s.toOption.foreach(_.merge.close())
  }
  def connect(paths: SocketPaths): Either[Throwable, Either[Socket, SocketChannel]] = {
    var s: Either[Socket, SocketChannel] = null
    try {
      s = SocketHandler.client(paths)
      Right(s)
    }
    catch {
      case e: SocketException =>
        if (s != null)
          s.merge.close()
        Left(e)
      case e: SocketExceptionLike =>
        if (s != null)
          s.merge.close()
        Left(e)
    }
  }
}
