package libdaemonjvm

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}
import java.nio.channels.{ClosedChannelException, FileLock}
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.StandardOpenOption
import scala.collection.JavaConverters._
import scala.util.Properties
import libdaemonjvm.server.LockError
import java.nio.channels.OverlappingFileLockException

final case class LockFiles(
  lockFile: Path,
  pidFile: Path,
  socketPaths: SocketPaths
) {
  def withLock[T](t: => Either[LockError, T]): Either[LockError, T] = {
    if (!Files.exists(lockFile)) {
      Files.createDirectories(lockFile.normalize.getParent)
      Files.write(lockFile, Array.emptyByteArray)
    }
    var c: FileChannel                                    = null
    var l: Either[OverlappingFileLockException, FileLock] = null
    try {
      c = FileChannel.open(lockFile, StandardOpenOption.WRITE)
      l =
        try Right(c.tryLock())
        catch {
          case ex: OverlappingFileLockException =>
            Left(ex)
        }
      l match {
        case Left(ex)    => Left(new LockError.Locked(lockFile, ex))
        case Right(null) => Left(new LockError.Locked(lockFile))
        case Right(_)    => t
      }
    }
    finally {
      if (l != null)
        try l.toOption.filter(_ != null).foreach(_.release())
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

  def under(dir: Path): LockFiles =
    under(dir, addPipePrefix = true)
  def under(dir: Path, addPipePrefix: Boolean): LockFiles =
    under(dir, addPipePrefix, checkPermissions = true)
  def under(
    dir: Path,
    addPipePrefix: Boolean,
    checkPermissions: Boolean
  ): LockFiles = {
    // FIXME Java 16 support on Windows also uses actual files on disk AFAIK.
    // So we might need to check permissions there too.
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
      socketPaths = SocketPaths(dir.resolve("socket"))
    )
  }
}
