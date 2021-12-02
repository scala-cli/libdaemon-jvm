package libdaemonjvm.tests

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import java.nio.channels.ServerSocketChannel

import libdaemonjvm._
import libdaemonjvm.internal._
import libdaemonjvm.server._
import java.net.ServerSocket
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
    LockFiles.under(
      dir.toNIO,
      "libdaemonjvm-tests-" + dir.segments.toVector.drop(dir.segmentCount - 2).mkString("-")
    )
  }

  private val count = new AtomicInteger
  def withTestDir[T](f: os.Path => T): T = {
    val dir = testDirBase / s"test-${count.incrementAndGet()}"
    try f(dir)
    finally os.remove.all(dir)
  }
  def tryAcquire[T](dir: os.Path)(f: (
    LockFiles,
    Either[LockError, Either[ServerSocket, ServerSocketChannel]]
  ) => T): T = {
    val files = lockFiles(dir)
    tryAcquire(files) { maybeChannel =>
      f(files, maybeChannel)
    }
  }
  def tryAcquire[T](files: LockFiles)(f: Either[
    LockError,
    Either[ServerSocket, ServerSocketChannel]
  ] => T): T =
    tryAcquire(files, LockProcess.default)(f)
  def tryAcquire[T](
    files: LockFiles,
    proc: LockProcess
  )(f: Either[LockError, Either[ServerSocket, ServerSocketChannel]] => T): T = {
    var serverChannel: Either[ServerSocket, ServerSocketChannel] = null
    var acceptThreadOpt                                          = Option.empty[Thread]
    val accepting                                                = new CountDownLatch(1)
    val shouldStop                                               = new AtomicBoolean(false)
    try {
      val maybeServerChannel = Lock.tryAcquire(files, proc) {
        serverChannel = SocketHandler.server(files.socketPaths)
        if (Properties.isWin)
          // Windows named pipes seem no to accept clients unless accept is being called on the server socket
          acceptThreadOpt =
            serverChannel.left.toOption.map(acceptAndDiscard(
              _,
              accepting,
              () => shouldStop.get()
            ))
        for (t <- acceptThreadOpt) {
          t.start()
          accepting.await()
          // waiting so that the accept call below effectively awaits client... :|
          Thread.sleep(
            1000L
          )
        }
        serverChannel
      }
      f(maybeServerChannel)
    }
    finally {
      shouldStop.set(true)
      try SocketFile.canConnect(files.socketPaths) // unblock the server thread last accept
      catch {
        case NonFatal(e) =>
          System.err.println(s"Ignoring $e while trying to unblock last accept")
      }
      for (channel <- Option(serverChannel))
        channel.merge.close()
    }
  }

  val acceptAndDiscardCount = new AtomicInteger
  def acceptAndDiscard(
    s: ServerSocket,
    accepting: CountDownLatch,
    shouldStop: () => Boolean
  ): Thread =
    new Thread(
      s"libdaemonjvm-tests-accept-and-discard-${acceptAndDiscardCount.incrementAndGet()}"
    ) {
      setDaemon(true)
      val closeCount = new AtomicInteger
      def closeSocket(socket: Socket): Unit = {
        val t = new Thread(s"$getName-close-${closeCount.incrementAndGet()}") {
          setDaemon(true)
          override def run(): Unit = {
            socket.close()
          }
        }
        t.start()
      }
      override def run(): Unit = {
        accepting.countDown()
        while (!shouldStop()) {
          val client = s.accept()
          // closing the client socket in the background, as this call seems to block a few seconds
          closeSocket(client)
        }
      }
    }

}
