import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.9:0.1.1`

import $file.jarmanifest
import $file.zip

import de.tobiasroeser.mill.vcs.version._
import mill._
import mill.scalalib._

import scala.concurrent.duration.{Duration, DurationInt}

def scala3        = "3.1.0"
def scala213      = "2.13.7"
def scala212      = "2.12.15"
def scalaVersions = Seq(scala3, scala213, scala212)

object library     extends Cross[Library](scalaVersions: _*)
object tests       extends Cross[Tests](scalaVersions: _*)
object `jni-tests` extends Cross[JniTests](scalaVersions: _*)

object library16 extends Cross[Library16](scalaVersions: _*)

def tmpDirBase =
  if (System.getenv("CI") == null)
    T.persistent {
      PathRef(T.dest / "working-dir")
    }
  else
    T {
      PathRef(os.home / ".test-data")
    }

def libraryDeps = Seq(
  ivy"dev.dirs:directories:26",
  ivy"io.get-coursier.jniutils:windows-jni-utils:0.3.1",
  ivy"com.github.alexarchambault.tmp.ipcsocket:ipcsocket:1.4.1-aa-2"
)

class Library(val crossScalaVersion: String) extends CrossScalaModule with LibDaemonPublish {
  def artifactName = "libdaemon"
  def ivyDeps      = super.ivyDeps() ++ libraryDeps
  def compileIvyDeps = super.compileIvyDeps() ++ Seq(
    ivy"org.graalvm.nativeimage:svm:21.2.0"
  )
  def javacOptions = super.javacOptions() ++ Seq(
    "--release",
    "8"
  )

  def baseJar = T {
    super.jar()
  }
  def mrJar = T {
    val newJar    = T.dest / "mr.jar"
    val baseJar0  = baseJar()
    val java16Jar = library16().jar()
    val extraEntries = zip.entries(java16Jar.path)
      .filter(_._1.getName.startsWith("libdaemonjvm"))
      .map {
        case (e, b) =>
          val e0 = zip.newEntry("META-INF/versions/16/" + e.getName, e)
          (e0, b)
      }
    assert(extraEntries.nonEmpty)
    val (manifestEntries, baseEntries) =
      zip.entries(baseJar0.path).partition(_._1.getName == "META-INF/MANIFEST.MF")
    assert(manifestEntries.length == 1)
    val (manifestEntry, manifestContent) = manifestEntries.head
    val updatedManifestContent = jarmanifest.addAttributes(manifestContent)(
      "Multi-Release" -> "true"
    )
    val allEntries = ((manifestEntry, updatedManifestContent) +: baseEntries) ++ extraEntries
    zip.write(newJar, allEntries)
    PathRef(newJar)
  }
  def jar = mrJar()
}

class Library16(val crossScalaVersion: String) extends CrossScalaModule {
  def moduleDeps = Seq(
    library()
  )
}

abstract class TestsBase extends CrossScalaModule { self =>
  def ivyDeps = super.ivyDeps() ++ libraryDeps
  trait Tests extends super.Tests {
    def unmanagedClasspath = super.unmanagedClasspath() ++ self.unmanagedClasspath()
    def ivyDeps = super.ivyDeps() ++ Seq(
      ivy"org.scalameta::munit:0.7.29",
      ivy"com.eed3si9n.expecty::expecty:0.15.4",
      ivy"com.lihaoyi::os-lib:0.7.8"
    )
    def testFramework = "munit.Framework"
    def forkEnv = super.forkEnv() ++ Seq(
      "TESTS_TMP_DIR" -> tmpDirBase().path.toString
    )
  }
}

class Tests(val crossScalaVersion: String) extends TestsBase {
  def unmanagedClasspath = super.unmanagedClasspath() ++ Seq(
    library().jar()
  )
  object test extends Tests
}

class JniTests(val crossScalaVersion: String) extends TestsBase {
  def unmanagedClasspath = super.unmanagedClasspath() ++ Seq(
    library().baseJar()
  )
  object test extends Tests {
    def sources = T.sources {
      tests().test.sources()
    }
  }
}

object manual extends Module {
  object server extends ScalaModule {
    def scalaVersion = scala3
    def moduleDeps   = Seq(library(scala3))
  }
  object client extends ScalaModule {
    def scalaVersion = scala3
    def moduleDeps   = Seq(library(scala3))
  }
}

def publishSonatype(tasks: mill.main.Tasks[PublishModule.PublishData]) =
  T.command {
    val timeout     = 10.minutes
    val credentials = sys.env("SONATYPE_USERNAME") + ":" + sys.env("SONATYPE_PASSWORD")
    val pgpPassword = sys.env("PGP_PASSWORD")
    val data        = define.Task.sequence(tasks.value)()

    doPublishSonatype(
      credentials = credentials,
      pgpPassword = pgpPassword,
      data = data,
      timeout = timeout,
      log = T.ctx().log
    )
  }

private def doPublishSonatype(
  credentials: String,
  pgpPassword: String,
  data: Seq[PublishModule.PublishData],
  timeout: Duration,
  log: mill.api.Logger
): Unit = {

  val artifacts = data.map {
    case PublishModule.PublishData(a, s) =>
      (s.map { case (p, f) => (p.path, f) }, a)
  }

  val isRelease = {
    val versions = artifacts.map(_._2.version).toSet
    val set      = versions.map(!_.endsWith("-SNAPSHOT"))
    assert(
      set.size == 1,
      s"Found both snapshot and non-snapshot versions: ${versions.toVector.sorted.mkString(", ")}"
    )
    set.head
  }
  val publisher = new publish.SonatypePublisher(
    uri = "https://oss.sonatype.org/service/local",
    snapshotUri = "https://oss.sonatype.org/content/repositories/snapshots",
    credentials = credentials,
    signed = true,
    gpgArgs = Seq(
      "--detach-sign",
      "--batch=true",
      "--yes",
      "--pinentry-mode",
      "loopback",
      "--passphrase",
      pgpPassword,
      "--armor",
      "--use-agent"
    ),
    readTimeout = timeout.toMillis.toInt,
    connectTimeout = timeout.toMillis.toInt,
    log = log,
    awaitTimeout = timeout.toMillis.toInt,
    stagingRelease = isRelease
  )

  publisher.publishAll(isRelease, artifacts: _*)
}

trait LibDaemonPublish extends PublishModule {
  import mill.scalalib.publish._
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "io.github.alexarchambault.libdaemon",
    url = s"https://github.com/alexarchambault/libdaemon-jvm",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("alexarchambault", "libdaemon-jvm"),
    developers = Seq(
      Developer("alexarchambault", "Alex Archambault", "https://github.com/alexarchambault")
    )
  )
  def publishVersion =
    finalPublishVersion()
}

private def computePublishVersion(state: VcsState, simple: Boolean): String =
  if (state.commitsSinceLastTag > 0)
    if (simple) {
      val versionOrEmpty = state.lastTag
        .filter(_ != "latest")
        .filter(_ != "nightly")
        .map(_.stripPrefix("v"))
        .flatMap { tag =>
          if (simple) {
            val idx = tag.lastIndexOf(".")
            if (idx >= 0)
              Some(tag.take(idx + 1) + (tag.drop(idx + 1).toInt + 1).toString + "-SNAPSHOT")
            else
              None
          }
          else {
            val idx = tag.indexOf("-")
            if (idx >= 0) Some(tag.take(idx) + "+" + tag.drop(idx + 1) + "-SNAPSHOT")
            else None
          }
        }
        .getOrElse("0.0.1-SNAPSHOT")
      Some(versionOrEmpty)
        .filter(_.nonEmpty)
        .getOrElse(state.format())
    }
    else {
      val rawVersion = os.proc("git", "describe", "--tags").call().out.text().trim
        .stripPrefix("v")
        .replace("latest", "0.0.0")
        .replace("nightly", "0.0.0")
      val idx = rawVersion.indexOf("-")
      if (idx >= 0) rawVersion.take(idx) + "+" + rawVersion.drop(idx + 1) + "-SNAPSHOT"
      else rawVersion
    }
  else {
    val fromTag = state
      .lastTag
      .getOrElse(state.format())
      .stripPrefix("v")
    if (fromTag == "0.0.0") "0.0.1-SNAPSHOT"
    else fromTag
  }

def finalPublishVersion = {
  val isCI = System.getenv("CI") != null
  if (isCI)
    T.persistent {
      val state = VcsVersion.vcsState()
      computePublishVersion(state, simple = false)
    }
  else
    T {
      val state = VcsVersion.vcsState()
      computePublishVersion(state, simple = true)
    }
}
