# libdaemon-jvm

*libdaemon-jvm* is a [libdaemon](http://0pointer.de/lennart/projects/libdaemon)-inspired
library for the JVM written in Scala.

It aims at making it easier for JVM-based daemon processes to
- ensure that a single instance of it is running at a time
- rely on domain sockets to listen to incoming connections

## Single process

*libdaemon-jvm* relies on Java file lock mechanism to ensure only a single instance
of a process is running at a time.

More concretely, it is passed a directory, where it writes or creates:
- a lock file
- a PID file
- a domain socket

It ensures that no-two processes relying on the same directory can run at a time, relying
on both the PID file and the domain socket to check for another running process.

## Domain sockets

*libdaemon-jvm* creates domain sockets using Unix domain socket support added in Java 16.

## Usage

Add the following dependency to your build
```text
io.github.alexarchambault.libdaemon::libdaemon:0.0.5
```
The latest version is [![Maven Central](https://img.shields.io/maven-central/v/io.github.alexarchambault.libdaemon/libdaemon_3.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.alexarchambault.libdaemon/libdaemon_3).

From the server, call `Lock.tryAcquire`, and start accepting connections on the server socket in the thunk passed to it:
```scala
import libdaemonjvm.server._
import java.nio.file._

val daemonDirectory: Path = ??? // pass a directory under the user home dir, computed with directories-jvm for example
val lockFiles = LockFiles.under(daemonDirectory)
val res = Lock.tryAcquire(lockFiles) { serverSocket: ServerSocketChannel =>
  // you should start listening on serverSocket here, and as much as possible,
  // only exit this block when you are actually accepting incoming connections
}
res match {
  case Left(ex: LockError.RecoverableError) => // something went wrong, you may want to retry after a small delay
  case Left(ex: LockError.FatalError) => // something went wrong, retrying makes less sense here
  case Right(_) => // daemon is listening on domain socket
}
```
