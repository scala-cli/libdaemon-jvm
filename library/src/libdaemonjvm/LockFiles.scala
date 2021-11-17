package libdaemonjvm

import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}
import java.nio.channels.FileLock
import java.nio.channels.ClosedChannelException
import java.io.IOException
import java.nio.file.StandardOpenOption

final case class LockFiles(
  lockFile: Path,
  pidFile: Path,
  socketPaths: SocketPaths
) {
  def withLock[T](t: => T): T = {
    if (!Files.exists(lockFile)) {
      Files.createDirectories(lockFile.normalize.getParent)
      Files.write(lockFile, Array.emptyByteArray)
    }
    var c: FileChannel = null
    var l: FileLock    = null
    try {
      c = FileChannel.open(lockFile, StandardOpenOption.WRITE)
      l = c.lock()
      t
    }
    finally {
      if (l != null)
        try l.release()
        catch {
          case _: ClosedChannelException =>
          case _: IOException            =>
        }
      if (c != null)
        c.close()
    }
  }
}

object LockFiles {
  def under(dir: Path, windowsPipeName: String): LockFiles =
    under(dir, windowsPipeName, addPipePrefix = true)
  def under(dir: Path, windowsPipeName: String, addPipePrefix: Boolean): LockFiles =
    LockFiles(
      lockFile = dir.resolve("lock"),
      pidFile = dir.resolve("pid"),
      socketPaths = SocketPaths(
        dir.resolve("socket"),
        if (addPipePrefix) "\\\\.\\pipe\\" + windowsPipeName else windowsPipeName
      )
    )
}
