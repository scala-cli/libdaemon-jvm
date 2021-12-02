package libdaemonjvm.internal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import org.scalasbt.ipcsocket.UnixDomainServerSocket;
import org.scalasbt.ipcsocket.UnixDomainSocket;
import org.scalasbt.ipcsocket.Win32NamedPipeServerSocket;
import org.scalasbt.ipcsocket.Win32NamedPipeSocket;
import org.scalasbt.ipcsocket.Win32SecurityLevel;

public final class SocketMaker {
  private static boolean isWin = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
  public static Socket client(String path) throws IOException {
    if (isWin) return new Win32NamedPipeSocket(path, true);
    else return new UnixDomainSocket(path, true);
  }
  public static ServerSocket server(String path) throws IOException {
    if (isWin) return new Win32NamedPipeServerSocket(path, true, Win32SecurityLevel.LOGON_DACL);
    else return new UnixDomainServerSocket(path, true);
  }
}
