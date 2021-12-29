package libdaemonjvm.server

import java.nio.channels.ServerSocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import libdaemonjvm.LockFiles
import libdaemonjvm.internal.{LockProcess, SocketFile, SocketHandler}
import java.net.ServerSocket

object Lock {

  def tryAcquire[T](
    files: LockFiles
  )(
    startListening: Either[ServerSocket, ServerSocketChannel] => T
  ): Either[LockError, T] =
    tryAcquire(files, LockProcess.default) {
      val socket = SocketHandler.server(files.socketPaths)
      startListening(socket)
    }

  def tryAcquire[T](
    files: LockFiles,
    proc: LockProcess
  )(
    setup: => T
  ): Either[LockError, T] = {

    def unsafeWritePidAndSetup(): Either[LockError, T] = {
      val pid = proc.pid()
      Files.write(files.pidFile, pid.toString.getBytes(StandardCharsets.UTF_8))
      Right(setup)
    }

    // the lock needs to have been acquired prior to running this
    def unsafeCleanUpAndSetup(): Either[LockError, T] = {

      def delete(file: Path): Either[LockError, Unit] =
        if (Files.exists(file)) {
          Files.delete(file)
          if (Files.exists(file)) // really necessary?
            Left(new LockError.CannotDeleteFile(file, null))
          else
            Right(())
        }
        else
          Right(())

      for {
        _ <- delete(files.socketPaths.path)
        _ <- delete(files.pidFile)
        t <- unsafeWritePidAndSetup()
      } yield t
    }

    def ifProcessRunning(pid: Int): Either[LockError, T] = {
      val err = SocketFile.canConnect(files.socketPaths) match {
        case Left(e) =>
          new LockError.ZombieFound(pid, e)
        case Right(()) =>
          new LockError.AlreadyRunning(pid)
      }
      Left(err)
    }

    def ifFiles(hasLock: Boolean): Either[LockError, T] = {
      val b = Files.readAllBytes(files.pidFile)

      // FIXME Catch malformed content errors here?
      val s = new String(b, StandardCharsets.UTF_8).trim()
      val pidOpt =
        if (s.nonEmpty && s.forall(_.isDigit)) Some(s.toInt)
        else None

      val maybeRes = pidOpt.flatMap { pid =>
        if (proc.isRunning(pid)) Some(ifProcessRunning(pid))
        else None
      }
      maybeRes.getOrElse {
        if (hasLock)
          unsafeCleanUpAndSetup()
        else
          files.withLock {
            unsafeCleanUpAndSetup()
          }
      }
    }

    def pidSocketFilesFound(): Boolean =
      Files.exists(files.pidFile) &&
      (SocketHandler.usesWindowsPipe(files.socketPaths) || Files.exists(files.socketPaths.path))

    if (pidSocketFilesFound())
      ifFiles(hasLock = false)
    else
      files.withLock {
        if (pidSocketFilesFound())
          ifFiles(hasLock = true)
        else
          unsafeCleanUpAndSetup()
      }
  }
}
