package libdaemonjvm

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}
import java.nio.channels.{ClosedChannelException, FileLock}
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.StandardOpenOption
import scala.collection.JavaConverters._
import scala.util.Properties

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

  private val forbiddenPermissions = {
    import PosixFilePermission._
    Set(
      GROUP_READ,
      GROUP_WRITE,
      GROUP_EXECUTE,
      OTHERS_READ,
      OTHERS_WRITE,
      OTHERS_EXECUTE
    )
  }

  def under(dir: Path, windowsPipeName: String): LockFiles =
    under(dir, windowsPipeName, addPipePrefix = true)
  def under(dir: Path, windowsPipeName: String, addPipePrefix: Boolean): LockFiles =
    under(dir, windowsPipeName, addPipePrefix, checkPermissions = true)
  def under(
    dir: Path,
    windowsPipeName: String,
    addPipePrefix: Boolean,
    checkPermissions: Boolean
  ): LockFiles = {
    if (checkPermissions && !Properties.isWin) {
      val perms   = Files.getPosixFilePermissions(dir).asScala.toSet
      val invalid = perms.intersect(forbiddenPermissions)
      if (invalid.nonEmpty)
        throw new IllegalArgumentException(
          s"$dir has invalid permissions ${invalid.map(_.name()).toVector.sorted.mkString(", ")}"
        )
    }
    LockFiles(
      lockFile = dir.resolve("lock"),
      pidFile = dir.resolve("pid"),
      socketPaths = SocketPaths(
        dir.resolve("socket"),
        if (addPipePrefix) "\\\\.\\pipe\\" + windowsPipeName else windowsPipeName
      )
    )
  }
}
