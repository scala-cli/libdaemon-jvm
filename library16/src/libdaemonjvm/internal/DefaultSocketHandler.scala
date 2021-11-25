package libdaemonjvm.internal

import scala.util.Properties

object DefaultSocketHandler {
  def default: SocketHandler =
    sys.props.get("libdaemonjvm.internal.DefaultSocketHandler") match {
      case Some("JniSocketHandler" | "jni")       => JniSocketHandler
      case Some("Java16SocketHandler" | "java16") => Java16SocketHandler
      case _                                      =>
        // On Windows, default to JNI for now when we can, as the JNI and Java 16
        // support aren't compatible with each other, so that Java 8 and Java 17 processes
        // can talk to each other (via the JNI stuff).
        // In more detail, JNI support relies on Windows "named pipes", while the Java 16
        // one relies on proper Unix-like domain socket support (added at some point in Windows
        // 10). Hence the incompatibility.
        if (Properties.isWin && JniSocketHandler.supported()) JniSocketHandler
        else Java16SocketHandler
    }
}
