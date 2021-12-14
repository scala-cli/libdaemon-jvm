package libdaemonjvm.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.posix.headers.Signal;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import java.nio.file.Path;

@TargetClass(className = "libdaemonjvm.internal.IsRunning")
@Platforms({Platform.LINUX.class, Platform.DARWIN.class})
final class IsRunningSubst {
  @Substitute
  Boolean isRunning(int pid) {
      return Signal.kill(pid, 0) == 0;
  }
}
