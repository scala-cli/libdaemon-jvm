package libdaemonjvm.internal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.scalasbt.ipcsocket.Win32NamedPipeServerSocket;
import org.scalasbt.ipcsocket.Win32NamedPipeSocket;

@TargetClass(className = "libdaemonjvm.internal.SocketMaker")
@Platforms({Platform.WINDOWS.class})
final class SocketMakerWindowsSubst {
  @Substitute
  public static Socket client(String path) throws IOException {
    return new Win32NamedPipeSocket(path, true);
  }
  @Substitute
  public static ServerSocket server(String path) throws IOException {
    return new Win32NamedPipeServerSocket(path, true);
  }
}
