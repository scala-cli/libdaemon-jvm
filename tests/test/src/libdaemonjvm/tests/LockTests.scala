package libdaemonjvm.tests

import com.eed3si9n.expecty.Expecty.expect
import libdaemonjvm._
import libdaemonjvm.internal._
import libdaemonjvm.server._

import java.nio.file.Files
import libdaemonjvm.client.Connect

class LockTests extends munit.FunSuite {

  test("simple") {
    TestUtil.withTestDir { dir =>
      val files = TestUtil.lockFiles(dir)
      TestUtil.tryAcquire(files) { maybeServerChannel =>
        expect(maybeServerChannel.isRight)
        val connectRes = Connect.tryConnect(files)
        expect(connectRes.exists(_.isRight))
      }
      val after = Connect.tryConnect(files)
      expect(after.exists(_.isLeft)) // zombie found (our own process, not listening on the socket)
    }
  }

  test("ignore PID file") {
    TestUtil.withTestDir { dir =>
      val files   = TestUtil.lockFiles(dir)
      val pidFile = os.Path(files.pidFile, os.pwd)
      os.makeDir.all(pidFile / os.up)
      os.write(pidFile, Array.emptyByteArray)
      TestUtil.tryAcquire(files) { maybeServerChannel =>
        expect(maybeServerChannel.isRight)
        val canConnect = SocketFile.canConnect(files.socketPaths)
        expect(canConnect.isRight)
      }
    }
  }

  if (!SocketHandler.usesWindowsPipe)
    test("ignore invalid socket file") {
      TestUtil.withTestDir { dir =>
        val files      = TestUtil.lockFiles(dir)
        val socketFile = os.Path(files.socketPaths.path, os.pwd)
        os.makeDir.all(socketFile / os.up)
        os.write(socketFile, Array.emptyByteArray)
        TestUtil.tryAcquire(files) { maybeServerChannel =>
          expect(maybeServerChannel.isRight)
          val canConnect = SocketFile.canConnect(files.socketPaths)
          expect(canConnect.isRight)
        }
      }
    }

  if (!SocketHandler.usesWindowsPipe)
    test("ignore PID file and invalid socket file") {
      TestUtil.withTestDir { dir =>
        val files = TestUtil.lockFiles(dir)

        val pidFile = os.Path(files.pidFile, os.pwd)
        os.makeDir.all(pidFile / os.up)
        os.write(pidFile, Array.emptyByteArray)

        val socketFile = os.Path(files.socketPaths.path, os.pwd)
        os.makeDir.all(socketFile / os.up)
        os.write(socketFile, Array.emptyByteArray)

        TestUtil.tryAcquire(files) { maybeServerChannel =>
          expect(maybeServerChannel.isRight)
          val canConnect = SocketFile.canConnect(files.socketPaths)
          expect(canConnect.isRight)
        }
      }
    }

  test("fail if already running") {
    TestUtil.withTestDir { dir =>
      val files = TestUtil.lockFiles(dir)
      TestUtil.tryAcquire(files) { maybeServerChannel =>
        expect(maybeServerChannel.isRight)
        val canConnect = SocketFile.canConnect(files.socketPaths)
        expect(canConnect.isRight)

        TestUtil.tryAcquire(files) { maybeNewChannel =>
          maybeNewChannel match {
            case Left(e: LockError.AlreadyRunning) =>
            case Left(otherError) =>
              throw new Exception("Unexpected error type (expected AlreadyRunning)", otherError)
            case Right(channel) =>
              sys.error("Opening new server channel should have failed")
          }
        }
      }
    }
  }

  test("succeed twice in a row") {
    def proc(pid0: Int): LockProcess =
      new LockProcess {
        def pid()               = pid0
        def isRunning(pid: Int) = pid == pid0
      }
    TestUtil.withTestDir { dir =>
      val files = TestUtil.lockFiles(dir)
      TestUtil.tryAcquire(files, proc(10)) { maybeChannel =>
        expect(maybeChannel.isRight)
        val canConnect = SocketFile.canConnect(files.socketPaths)
        expect(canConnect.isRight)
      }
      TestUtil.tryAcquire(files, proc(20)) { maybeChannel =>
        expect(maybeChannel.isRight)
        val canConnect = SocketFile.canConnect(files.socketPaths)
        expect(canConnect.isRight)
      }
    }
  }

  test("spot zombie") {
    def proc(pid0: Int, alsoRunning: Set[Int] = Set.empty): LockProcess =
      new LockProcess {
        def pid()               = pid0
        def isRunning(pid: Int) = pid == pid0 || alsoRunning(pid)
      }
    TestUtil.withTestDir { dir =>
      val files = TestUtil.lockFiles(dir)
      TestUtil.tryAcquire(files, proc(10)) { maybeChannel =>
        expect(maybeChannel.isRight)
        val canConnect = SocketFile.canConnect(files.socketPaths)
        expect(canConnect.isRight)
      }
      TestUtil.tryAcquire(files, proc(20, Set(10))) { maybeChannel =>
        maybeChannel match {
          case Left(e: LockError.ZombieFound) =>
          case Left(otherError) =>
            throw new Exception("Unexpected error type (expected ZombieFound)", otherError)
          case Right(channel) =>
            sys.error("Opening new server channel should have failed")
        }
      }
    }
  }

}
