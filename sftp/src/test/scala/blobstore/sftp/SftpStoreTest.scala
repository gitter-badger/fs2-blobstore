/*
Copyright 2018 LendUp Global, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package blobstore
package sftp

import java.nio.file.{Files, Paths}

import cats.effect.IO
import com.jcraft.jsch.{ChannelSftp, JSch}
import scala.util.control.NonFatal

@org.scalatest.Ignore
class SftpStoreTest extends AbstractStoreTest {

  val (channel, session) = try {
    val jsch = new JSch()
    jsch.setKnownHosts("~/.ssh/known_hosts")
    jsch.addIdentity("~/.ssh/id_rsa_tmp")

    val session = jsch.getSession("127.0.0.1")
    session.setTimeout(10000)
    session.connect()

    val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    channel.connect(10000)
    (channel, session)
  } catch {
    case _: Throwable =>  (null, null)
  }

  private val rootDir = Paths.get("tmp/sftp-store-root/").toAbsolutePath.normalize
  override val store: Store[IO] = SftpStore(rootDir.toString, channel)
  override val root: String = "sftp_tests"


  // remove dirs created by AbstractStoreTest
  override def afterAll(): Unit = {
    super.afterAll()

    try {
      channel.disconnect()
      session.disconnect()
    } catch {
      case _: Throwable =>
    }

    val clean = List("all", "list-many", "move-keys", "list-dirs/subdir", "list-dirs",
      "transfer-dir-to-dir-dst", "transfer-file-to-file-dst", "transfer-single-file-to-dir-dst",
      "transfer-dir-rec-dst/subdir/", "transfer-dir-rec-dst")
      .map(t => rootDir.resolve(s"$root/test-$testRun/$t")) ++
      List(rootDir.resolve(s"$root/test-$testRun"), rootDir.resolve(s"$root"), rootDir)

    clean.foreach(p => try { Files.delete(p) } catch { case NonFatal(_) => /* noop */ })

  }

}