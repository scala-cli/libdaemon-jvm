package libdaemonjvm.internal

import scala.util.Properties

object DefaultSocketHandler {
  def default: SocketHandler =
    Java16SocketHandler
}
