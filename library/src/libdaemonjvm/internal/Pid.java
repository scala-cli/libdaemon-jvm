package libdaemonjvm.internal;

import java.lang.management.ManagementFactory;

public class Pid {
  public Integer get() {
    try {
      String name = ManagementFactory.getRuntimeMXBean().getName();
      int idx = name.indexOf('@');
      String pidStr = name.substring(0, idx);
      return Integer.parseInt(pidStr);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
