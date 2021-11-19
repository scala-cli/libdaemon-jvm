package libdaemonjvm

import java.nio.file.Paths

import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger

import libdaemonjvm.internal.SocketFile
import libdaemonjvm.server.Lock
import java.io.IOException

object TestServer {
  val delay          = 2.seconds
  def runTestClients = false
  def main(args: Array[String]): Unit = {
    val files = LockFiles.under(Paths.get("data-dir"), "libdaemonjvm\\test-server-client\\pipe")
    val incomingConn = Lock.tryAcquire(files) match {
      case Left(e)         => throw e
      case Right(Left(s))  => () => s.accept()
      case Right(Right(s)) => () => s.accept()
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
      t.setDaemon(true)
      t.start()
    }
  }
}
