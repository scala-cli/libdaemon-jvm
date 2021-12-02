# libdaemon-jvm

*libdaemon-jvm* is a [libdaemon](http://0pointer.de/lennart/projects/libdaemon)-inspired
library for the JVM written in Scala.

It aims at making it easier for JVM-based daemon processes to
- ensure that a single instance of it is running at a time
- rely on Unix domain sockets (or Windows named pipes) to listen to incoming connections

## Single process

*libdaemon-jvm* relies on Java file lock mechanism to ensure only a single instance
of a process is running at a time.

More concretely, it is passed a directory, where it writes or creates:
- a lock file
- a PID file
- a domain socket (except when named pipes are used on Windows)

It ensures that no-two processes relying on the same directory can run at a time, relying
on both the PID file and the domain socket to check for another running process.

## Domain sockets

*libdaemon-jvm* creates Unix domain sockets or Windows named pipes using either
- the JNI Unix domain socket and Windows named pipe support in the [ipcsocket](https://github.com/sbt/ipcsocket) library
- Unix domain socket support in Java >= 16

The ipcsocket library JNI support is only available on Linux / macOS / Windows for the
x86_64 architecture, and macOS for the ARM64 architecture (untested). For other OSes and
architectures, Java >= 16 is required.

On Windows on x86_64, *libdaemon-jvm* defaults to using ipcsocket JNI-based Windows named pipes.
On Windows but on a different architecture, it defaults to the Unix domain socket support of
Java >= 16, that happens to also work on Windows (requires a not-too-dated Windows 10 version),
but is incompatible with Windows named pipes.

On other OSes, when using Java >= 16, *libdaemon-jvm* defaults to Java's own Unix domain socket
support. On Java < 16, it only supports Linux on x86_64, or macOS on x86_64 or ARM64. Java >= 16
and ipcsocket JNI-based sockets can talk to each other on the same machine (no hard requirement
to use Java >= 16 for both clients and servers).

In all cases, when Java < 16 is supported, both Java >= 16 and Java < 16 clients and servers
can talk to each other.

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
val lockFiles = LockFiles.under(daemonDirectory, "my-app-name\\daemon") // second argument is the Windows named pipe path (that doesn't live in the file system)
val res = Lock.tryAcquire(lockFiles) { serverSocket: Either[ServerSocket, ServerSocketChannel] =>
  // serverSocket is a Right(…) when Java >= 16 Unix domain socket support is used,
  // it's Left(…) when ipcsocket JNI support is used

  // you should start listening on serverSocket here, and as much as possible,
  // only exit this block when you are actually accepting incoming connections
}
res match {
  case Left(ex: LockError.RecoverableError) => // something went wrong, you may want to retry after a small delay
  case Left(ex: LockError.FatalError) => // something went wrong, retrying makes less sense here
  case Right(_) => // daemon is listening on Unix domain socket or Windows named pipe
}
```
