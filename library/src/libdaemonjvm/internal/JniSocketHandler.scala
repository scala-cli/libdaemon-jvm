package libdaemonjvm.internal

import java.nio.channels.{ServerSocketChannel, SocketChannel}
import java.nio.file.Path

import org.scalasbt.ipcsocket.{UnixDomainServerSocket, UnixDomainSocket}
import java.net.Socket
import java.net.ServerSocket
import java.io.IOException
import org.scalasbt.ipcsocket.NativeErrorException
import scala.util.Properties
import org.scalasbt.ipcsocket.Win32NamedPipeSocket
import org.scalasbt.ipcsocket.Win32NamedPipeServerSocket

import libdaemonjvm.errors._
import libdaemonjvm.SocketPaths
import java.util.Locale

object JniSocketHandler extends SocketHandler {

  def supported(): Boolean =
    (Properties.isLinux || Properties.isWin || Properties.isMac) &&
    sys.props.get("os.arch").exists { arch =>
      val arch0 = arch.toLowerCase(Locale.ROOT)
      arch0 == "x86_64" || arch0 == "amd64"
    }

  private val connectionRelatedCodes = Set(61, 111)
  private def exHandler: PartialFunction[Throwable, Nothing] = {
    case ex: IOException
        if Option(ex.getCause)
          .collect { case e: NativeErrorException => e }
          .exists(e => connectionRelatedCodes(e.returnCode)) =>
      throw new ConnectExceptionLike(ex)
    case ex: IOException if ex.getMessage.contains("error code 2") =>
      throw new SocketExceptionLike(ex)
    case ex: IOException if ex.getMessage.contains("Couldn't open pipe for") =>
      throw new SocketExceptionLike(ex)
  }

  private def actualPath(paths: SocketPaths): String =
    if (usesWindowsPipe) paths.windowsPipeName
    else paths.path.toString

  def usesWindowsPipe: Boolean =
    Properties.isWin

  def client(paths: SocketPaths): Either[Socket, SocketChannel] = {
    val s =
      try SocketMaker.client(actualPath(paths))
      catch exHandler
    Left(s)
  }

  def server(paths: SocketPaths): Either[ServerSocket, ServerSocketChannel] = {
    val s = SocketMaker.server(actualPath(paths))
    Left(s)
  }
}
