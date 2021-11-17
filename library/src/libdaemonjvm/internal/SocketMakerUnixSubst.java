package libdaemonjvm.internal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.scalasbt.ipcsocket.UnixDomainServerSocket;
import org.scalasbt.ipcsocket.UnixDomainSocket;

@TargetClass(className = "libdaemonjvm.internal.SocketMaker")
@Platforms({Platform.LINUX.class, Platform.DARWIN.class})
final class SocketMakerUnixSubst {
  @Substitute
  public static Socket client(String path) throws IOException {
    return new UnixDomainSocket(path, true);
  }
  @Substitute
  public static ServerSocket server(String path) throws IOException {
    return new UnixDomainServerSocket(path, true);
  }
}
