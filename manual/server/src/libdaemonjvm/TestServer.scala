package libdaemonjvm

import java.io.{Closeable, IOException}
import java.nio.file.{Files, Paths}
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Properties

import libdaemonjvm.internal.SocketFile
import libdaemonjvm.server.Lock

object TestServer {
  val delay          = 2.seconds
  def runTestClients = false

  def runServer(incomingConn: () => Closeable): Unit = {
    val count = new AtomicInteger
    while (true) {
      println("Waiting for clients")
      val c   = incomingConn()
      val idx = count.incrementAndGet()
      val runnable: Runnable = { () =>
        println(s"New incoming connection $idx, closing it in $delay")
        Thread.sleep(delay.toMillis)
        println(s"Closing incoming connection $idx")
        c.close()
        println(s"Closed incoming connection $idx")
      }
      val t = new Thread(runnable)
      t.start()
      Thread.sleep(1000L) // meh, wait for server to be actually listening
    }
  }

  def main(args: Array[String]): Unit = {
    val path = Paths.get("data-dir")
    if (!Properties.isWin) {
      Files.createDirectories(path)
      Files.setPosixFilePermissions(
        path,
        Set(
          PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE,
          PosixFilePermission.OWNER_EXECUTE
        ).asJava
      )
    }
    val files = LockFiles.under(path)
    Lock.tryAcquire(files)(s => runServer(() => s.accept())) match {
      case Left(e)   => throw e
      case Right(()) =>
    }

    def clientRunnable(idx: Int): Runnable = { () =>
      println(s"Waiting 5s to connect client $idx to server")
      Thread.sleep(5000L)
      println(s"Trying to connect client $idx to server")
      val canConnect = SocketFile.canConnect(files.socketPaths)
      println(s"canConnect $idx: $canConnect")
      canConnect match {
        case Left(ex: IOException) if ex.getMessage.contains("All pipe instances are busy") =>
          Thread.sleep(100L)
          clientRunnable(idx).run()
        case _ =>
      }
    }
    def runClient(idx: Int): Unit = {
      val clientThread = new Thread(clientRunnable(idx))
      clientThread.setDaemon(true)
      clientThread.start()
    }
    if (runTestClients) {
      runClient(1)
      runClient(2)
      runClient(3)
      runClient(4)
    }
  }
}
