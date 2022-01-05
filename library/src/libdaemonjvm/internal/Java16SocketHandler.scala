package libdaemonjvm.internal

import java.io.IOException
import java.net.{StandardProtocolFamily, UnixDomainSocketAddress}
import java.nio.channels.SocketChannel
import java.nio.file.Path

import scala.util.control.NonFatal
import java.nio.channels.ServerSocketChannel
import java.nio.file.Paths

import libdaemonjvm.SocketPaths

object Java16SocketHandler extends SocketHandler {
  def usesWindowsPipe: Boolean = false

  def client(paths: SocketPaths): SocketChannel = {
    val a                = UnixDomainSocketAddress.of(paths.path)
    var s: SocketChannel = null
    try {
      s = SocketChannel.open(StandardProtocolFamily.UNIX)
      s.connect(a)
      s.finishConnect()
      s
    }
    catch {
      case NonFatal(ex) =>
        if (s != null)
          try s.close()
          catch {
            case e0: IOException =>
              ex.addSuppressed(e0) // should we?
          }
        throw ex
    }
  }

  def server(paths: SocketPaths): ServerSocketChannel = {
    val a = UnixDomainSocketAddress.of(paths.path)
    val s = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
    s.bind(a)
    s
  }
}
