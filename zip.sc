import java.io._
import java.nio.file.Files
import java.util.zip._

import scala.collection.JavaConverters._

def newEntry(name: String, basedOn: ZipEntry): ZipEntry = {
  val e = new ZipEntry(name)
  e.setComment(e.getComment)
  e.setCompressedSize(e.getCompressedSize)
  if (e.getCrc >= 0)
    e.setCrc(e.getCrc)
  for (t <- Option(e.getCreationTime))
    e.setCreationTime(t)
  e.setExtra(e.getExtra)
  for (t <- Option(e.getLastAccessTime))
    e.setLastAccessTime(t)
  for (t <- Option(e.getLastModifiedTime))
    e.setLastModifiedTime(t)
  if (e.getMethod >= 0)
    e.setMethod(e.getMethod)
  if (e.getSize >= 0)
    e.setSize(e.getSize)
  e.setTime(e.getTime)
  e.setTimeLocal(e.getTimeLocal)
  e
}
def entries(file: os.Path): Seq[(ZipEntry, Array[Byte])] = {
  var zf: ZipFile = null
  try {
    zf = new ZipFile(file.toIO)
    zf.entries().asScala.toVector.map { e =>
      val baos            = new ByteArrayOutputStream
      var is: InputStream = null
      try {
        is = zf.getInputStream(e)
        os.Internals.transfer(is, baos)
      }
      finally if (is != null)
        is.close()
      (e, baos.toByteArray)
    }
  }
  finally if (zf != null)
    zf.close()
}
def write(dest: os.Path, entries: Seq[(ZipEntry, Array[Byte])]): Unit = {
  var fos: OutputStream    = null
  var zos: ZipOutputStream = null
  try {
    fos = Files.newOutputStream(dest.toNIO)
    zos = new ZipOutputStream(fos)
    for ((e, b) <- entries) {
      zos.putNextEntry(e)
      zos.write(b)
    }
    zos.finish()
  }
  finally {
    if (zos != null)
      zos.close()
    if (fos != null)
      fos.close()
  }
}
