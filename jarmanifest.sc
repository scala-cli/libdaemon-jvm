import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.jar._

def addAttributes(content: Array[Byte])(attributes: (String, String)*): Array[Byte] = {
  val m    = new Manifest(new ByteArrayInputStream(content))
  val attr = m.getMainAttributes
  attr.put(Attributes.Name.MULTI_RELEASE, "true")
  val baos = new ByteArrayOutputStream
  m.write(baos)
  baos.toByteArray
}
