package libdaemonjvm

import java.net.{ServerSocket, Socket}
import java.nio.ByteBuffer
import java.nio.channels.{Channels, ReadableByteChannel, ServerSocketChannel, SocketChannel}

object Util {

  def socketFromChannel(channel: SocketChannel): Socket =
    new Socket {
      override def getInputStream() =
        Channels.newInputStream(
          // Passing a custom ReadableByteChannel rather than channel,
          // so that ChannelInputStream doesn't try to acquire the "blocking lock",
          // that's also acquired by output stream stuff below (which causes reads and
          // writes to block each other).
          new ReadableByteChannel {
            override def read(dst: ByteBuffer) =
              channel.read(dst)
            override def close() =
              channel.close()
            override def isOpen() =
              channel.isOpen()
          }
        )
      override def getOutputStream() =
        Channels.newOutputStream(channel)
      // override def getLocalAddress() =
      //   channel.getLocalAddress()
      override def isConnected() =
        channel.isConnected()
      override def getRemoteSocketAddress() =
        channel.getRemoteAddress()
      override def close() =
        channel.close()
      override def shutdownInput() =
        channel.shutdownInput()
      override def shutdownOutput() =
        channel.shutdownOutput()
    }

  def serverSocketFromChannel(serverChannel: ServerSocketChannel): ServerSocket =
    new ServerSocket {
      override def accept() =
        socketFromChannel(serverChannel.accept())
      override def close() =
        serverChannel.close()
      override def getLocalPort() = -1
    }
}
