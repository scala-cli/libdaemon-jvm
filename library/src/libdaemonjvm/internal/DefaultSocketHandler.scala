package libdaemonjvm.internal

object DefaultSocketHandler {
  def default: SocketHandler =
    JniSocketHandler
}
