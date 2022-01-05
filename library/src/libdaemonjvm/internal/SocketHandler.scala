package libdaemonjvm.internal

import java.nio.channels.{ServerSocketChannel, SocketChannel}
import java.nio.file.Path

import libdaemonjvm.SocketPaths

trait SocketHandler {
  def usesWindowsPipe: Boolean
  def client(paths: SocketPaths): SocketChannel
  def server(paths: SocketPaths): ServerSocketChannel
}

object SocketHandler {
  def usesWindowsPipe: Boolean =
    DefaultSocketHandler.default.usesWindowsPipe
  def client(paths: SocketPaths): SocketChannel =
    DefaultSocketHandler.default.client(paths)
  def server(paths: SocketPaths): ServerSocketChannel =
    DefaultSocketHandler.default.server(paths)
}
