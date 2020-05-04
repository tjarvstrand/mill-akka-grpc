/*
mill-akka-grpc

Copyright (c) 2020 Thomas Järvstrand (tjarvstrand__at__gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
*/
import mill._, scalalib._, publish._

import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest:0.2.1`
import de.tobiasroeser.mill.integrationtest._

def millVersion = "0.6.1"

object `mill-akka-grpc` extends Cross[MillAkkaGrpc]("2.12.4")
class MillAkkaGrpc(val crossScalaVersion: String) extends CrossScalaModule with PublishModule {
//object `mill-akka-grpc` extends ScalaModule with PublishModule {
//  def scalaVersion = "2.12.4"
  def publishVersion = "0.1.0"
  def mainClass = Some("com.github.tjarvstrand.mill.AkkaGrpcGenerator")

  override def artifactName: T[String] = "mill-akka-grpc"

  def pomSettings = PomSettings(
    description = "Mill module for Akka GRPC",
    organization = "com.github.tjarvstrand",
    url = "https://github.com/tjarvstrand/mill-akka-grpc",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("tjarvstrand", "mill-akka-grpc"),
    developers = Seq(
      Developer("tjarvstrand", "Thomas Järvstrand","https://github.com/tjarvstrand")
    )
  )

  override def ivyDeps = Agg(
    ivy"com.lightbend.akka.grpc::akka-grpc-codegen:0.8.4",
    ivy"com.github.os72:protoc-jar:3.11.4",
    ivy"com.lihaoyi::mill-scalalib:0.6.2",
    ivy"com.lihaoyi::os-lib:0.6.2"
  )
}

object itest extends MillIntegrationTestModule {
  def millTestVersion = T {
    val ctx = T.ctx()
    ctx.env.get("TEST_MILL_VERSION").filterNot(_.isEmpty).getOrElse(millVersion)
  }
  def pluginsUnderTest = Seq(`mill-akka-grpc`("2.12.4"))
}