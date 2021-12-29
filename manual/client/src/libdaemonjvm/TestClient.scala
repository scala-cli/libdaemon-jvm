package libdaemonjvm

import java.nio.file.Paths

import libdaemonjvm.internal.SocketFile
import java.security.SecureRandom

object TestClient {
  def main(args: Array[String]): Unit = {
    val files      = LockFiles.under(Paths.get("data-dir"))
    val canConnect = SocketFile.canConnect(files.socketPaths)
    println(s"canConnect: $canConnect")
  }
}
