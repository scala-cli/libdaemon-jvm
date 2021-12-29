package libdaemonjvm.internal

import org.scalasbt.ipcsocket.UnixDomainServerSocket
import java.util.concurrent.atomic.AtomicInteger
import scala.reflect.Selectable.reflectiveSelectable
import org.scalasbt.ipcsocket._

class CustomUnixDomainServerSocket(path: String) extends UnixDomainServerSocket(path, true) {
  override def close(): Unit = {
    val cls = classOf[UnixDomainServerSocket]
    System.err.println("Fields:")
    for (f <- cls.getDeclaredFields)
      System.err.println(s"  $f")
    val fld = cls.getDeclaredField("fd")
    fld.setAccessible(true)
    val fd = fld.get(this).asInstanceOf[AtomicInteger].get()
    UnixDomainSocketLibraryProvider.get(true).shutdown(fd, 1)
    super.close()
  }
}