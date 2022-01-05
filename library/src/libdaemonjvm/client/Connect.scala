package libdaemonjvm.client

import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import libdaemonjvm.internal.{LockProcess, SocketFile, SocketHandler}
import libdaemonjvm.LockFiles

object Connect {

  def tryConnect(files: LockFiles): Option[Either[ConnectError, SocketChannel]] =
    tryConnect(files, LockProcess.default)

  def tryConnect(
    files: LockFiles,
    proc: LockProcess
  ): Option[Either[ConnectError, SocketChannel]] = {

    def ifProcessRunning(pid: Int): Either[ConnectError, SocketChannel] =
      SocketFile.connect(files.socketPaths)
        .left.map(e => new ConnectError.ZombieFound(pid, e))

    def ifFiles(hasLock: Boolean): Option[Either[ConnectError, SocketChannel]] = {
      val b = Files.readAllBytes(files.pidFile)

      // FIXME Catch malformed content errors here?
      val s = new String(b, StandardCharsets.UTF_8).trim()
      val pidOpt =
        if (s.nonEmpty && s.forall(_.isDigit)) Some(s.toInt)
        else None

      pidOpt.flatMap { pid =>
        if (proc.isRunning(pid)) Some(ifProcessRunning(pid))
        else None
      }
    }

    def pidSocketFilesFound(): Boolean =
      Files.exists(files.pidFile) &&
      (SocketHandler.usesWindowsPipe || Files.exists(files.socketPaths.path))

    if (pidSocketFilesFound())
      ifFiles(hasLock = false)
    else
      None
  }
}
