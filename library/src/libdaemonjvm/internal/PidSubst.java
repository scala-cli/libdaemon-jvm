package libdaemonjvm.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.posix.headers.Signal;
import com.oracle.svm.core.posix.headers.Unistd;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import java.nio.file.Path;

@TargetClass(className = "libdaemonjvm.internal.Pid")
@Platforms({Platform.LINUX.class, Platform.DARWIN.class})
final class PidSubst {
  @Substitute
  Integer get() {
      return Unistd.getpid();
  }
}
