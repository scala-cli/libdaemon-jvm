package libdaemonjvm.internal

import java.nio.channels.{ServerSocketChannel, SocketChannel}
import java.nio.file.Path

import java.net.Socket
import java.net.ServerSocket

import libdaemonjvm.SocketPaths

trait SocketHandler {
  def usesWindowsPipe: Boolean
  def client(paths: SocketPaths): Either[Socket, SocketChannel]
  def server(paths: SocketPaths): Either[ServerSocket, ServerSocketChannel]
}

object SocketHandler {
  def usesWindowsPipe: Boolean =
    DefaultSocketHandler.default.usesWindowsPipe
  def client(paths: SocketPaths): Either[Socket, SocketChannel] =
    DefaultSocketHandler.default.client(paths)
  def server(paths: SocketPaths): Either[ServerSocket, ServerSocketChannel] =
    DefaultSocketHandler.default.server(paths)
}
