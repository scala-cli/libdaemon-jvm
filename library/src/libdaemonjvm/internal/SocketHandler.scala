package libdaemonjvm.internal

import java.nio.channels.{ServerSocketChannel, SocketChannel}
import java.nio.file.Path

import java.net.Socket
import java.net.ServerSocket

import libdaemonjvm.SocketPaths

trait SocketHandler {
  def supportsWindowsPipe: Boolean
  def usesWindowsPipe(preferWindowsPipe: Boolean): Boolean =
    supportsWindowsPipe && preferWindowsPipe
  def usesWindowsPipe(paths: SocketPaths): Boolean =
    usesWindowsPipe(paths.preferWindowsPipes)
  def client(paths: SocketPaths): Either[Socket, SocketChannel]
  def server(paths: SocketPaths): Either[ServerSocket, ServerSocketChannel]
}

object SocketHandler {
  def supportsWindowsPipe: Boolean =
    DefaultSocketHandler.default.supportsWindowsPipe
  def usesWindowsPipe(preferWindowsPipe: Boolean): Boolean =
    DefaultSocketHandler.default.usesWindowsPipe(preferWindowsPipe)
  def usesWindowsPipe(paths: SocketPaths): Boolean =
    DefaultSocketHandler.default.usesWindowsPipe(paths)
  def client(paths: SocketPaths): Either[Socket, SocketChannel] =
    DefaultSocketHandler.default.client(paths)
  def server(paths: SocketPaths): Either[ServerSocket, ServerSocketChannel] =
    DefaultSocketHandler.default.server(paths)
}
