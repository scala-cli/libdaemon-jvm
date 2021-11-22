package libdaemonjvm.errors

import java.io.IOException

class SocketExceptionLike(cause: Throwable) extends IOException(cause)
