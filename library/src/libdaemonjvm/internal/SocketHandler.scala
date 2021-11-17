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

import libdaemonjvm.SocketPaths

object SocketHandler {
  private val connectionRelatedCodes = Set(61, 111)
  private def exHandler: PartialFunction[Throwable, Nothing] = {
    case ex: IOException
        if Option(ex.getCause)
          .collect { case e: NativeErrorException => e }
          .exists(e => connectionRelatedCodes(e.returnCode)) =>
      throw new SocketExceptionLike(ex)
    case ex: IOException if ex.getMessage.contains("error code 2") =>
      throw new SocketExceptionLike(ex)
    case ex: IOException if ex.getMessage.contains("Couldn't open pipe for") =>
      throw new SocketExceptionLike(ex)
  }

  private def actualPath(paths: SocketPaths): String =
    if (usesWindowsPipe) paths.windowsPipeName
    // val bt = "\\"
    // s"$bt$bt.${bt}pipe$bt" + f
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
