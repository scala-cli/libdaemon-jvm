package libdaemonjvm

import java.nio.file.Path

final case class SocketPaths(
  path: Path,
  windowsPipeName: String,
  preferWindowsPipes: Boolean
)
