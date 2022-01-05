package libdaemonjvm.tests

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import java.nio.channels.ServerSocketChannel

import libdaemonjvm._
import libdaemonjvm.internal._
import libdaemonjvm.server._
import scala.util.Properties
import java.util.concurrent.CountDownLatch
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import libdaemonjvm.internal.SocketFile
import scala.util.control.NonFatal

object TestUtil {
  private lazy val testDirBase = {
    val baseDirStr = Option(System.getenv("TESTS_TMP_DIR")).getOrElse {
      sys.error("libdaemon.test-dir-base not set")
    }
    val baseDir = os.Path(baseDirStr, os.pwd)
    val rng     = new SecureRandom
    val dir     = baseDir / s"run-${math.abs(rng.nextInt().toLong)}"
    os.makeDir.all(dir)
    Runtime.getRuntime.addShutdownHook(
      new Thread {
        override def run() =
          os.remove.all(dir)
      }
    )
    dir
  }

  def lockFiles(dir: os.Path): LockFiles = {
    if (!Properties.isWin) {
      os.makeDir.all(dir)
      os.perms.set(dir, "rwx------")
    }
    LockFiles.under(dir.toNIO)
  }

  private val count = new AtomicInteger
  def withTestDir[T](f: os.Path => T): T = {
    val dir = testDirBase / s"test-${count.incrementAndGet()}"
    try f(dir)
    finally os.remove.all(dir)
  }
  def tryAcquire[T](dir: os.Path)(f: (
    LockFiles,
    Either[LockError, ServerSocketChannel]
  ) => T): T = {
    val files = lockFiles(dir)
    tryAcquire(files) { maybeChannel =>
      f(files, maybeChannel)
    }
  }
  def tryAcquire[T](files: LockFiles)(f: Either[LockError, ServerSocketChannel] => T): T =
    tryAcquire(files, LockProcess.default)(f)
  def tryAcquire[T](
    files: LockFiles,
    proc: LockProcess
  )(f: Either[LockError, ServerSocketChannel] => T): T = {
    var serverChannel: ServerSocketChannel = null
    val accepting                          = new CountDownLatch(1)
    try {
      val maybeServerChannel = Lock.tryAcquire(files, proc) {
        serverChannel = SocketHandler.server(files.socketPaths)
        serverChannel
      }
      f(maybeServerChannel)
    }
    finally {
      try SocketFile.canConnect(files.socketPaths) // unblock the server thread last accept
      catch {
        case NonFatal(e) =>
          System.err.println(s"Ignoring $e while trying to unblock last accept")
      }
      for (channel <- Option(serverChannel))
        channel.close()
    }
  }
}
