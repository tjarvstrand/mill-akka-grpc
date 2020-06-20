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
package com.github.tjarvstrand.mill

import akka.grpc.gen.CodeGenerator.ScalaBinaryVersion
import com.github.tjarvstrand.mill.Language.Scala
import coursier.MavenRepository
import mill._
import mill.api.Result
import mill.api.Result.Failure
import mill.api.Result.Success
import mill.define.Sources
import mill.scalalib._
import os.Path
import upickle.default._


trait AkkaGrpcScalaModule extends ScalaModule {

  def codeGenerationType: T[CodeGenerationType]

  /**
   * @return The directories where the module's protobuf definitions are located
   */
  def protoDirs: Seq[Path] = Seq(
      millSourcePath / "src" / "main" / "protobuf",
      millSourcePath / "protobuf"
    ).filter(os.exists)

  /**
   * @return Additional directories where protobuf include files should be searched for
   */
  def protoIncludeDirs: T[Seq[Path]] = T(Seq.empty[Path])

  /**
   * @return version of protoc to use
   */
  def protocVersion: T[String] = T("3.11.4")

  /**
   * @return additional options to pass to protoc
   */
  def protocOptions: T[Seq[String]] = T(Seq.empty[String])

  /**
   * @return Whether or not to include the standard proto types from Google
   */
  def includeStandardTypes: T[Boolean] = T(true)

  /**
   * @return Additional options to the akka-grpc generators
   */
  def generatorOptions: T[Seq[String]] = T(Seq.empty[String])

  /**
   * @return Version of the akka-grpc runtime to use
   */
  def akkaGrpcVersion: T[String] = T("1.0.0")

  /**
   * @return Version of the grpc library to use
   */
  def grpcVersion: T[String] = T {
    Lib.resolveDependencies(
      Seq(
        coursier.LocalRepositories.ivy2Local,
        MavenRepository("https://repo1.maven.org/maven2")
      ),
      Lib.depToDependency(_, scalaVersion()),
      Seq(ivy"com.lightbend.akka.grpc::akka-grpc-runtime:${akkaGrpcVersion()}")
    ).map {
      _.iterator.find(_.path.last.matches("""grpc-core-[0-9.]+\.jar"""))
        .map { _.path.baseName.split("-").last }
    } match {
      case
        Success(Some(version)) => Success(version)
      case _ =>
        Failure("Unable to deduce grpc version based on dependencies. Consider overriding grpcVersion explicitly")
    }
  }

  override def generatedSources = T { super.generatedSources() :+ compileAkkaGrpc() }

  override def sources: Sources = T.sources { super.sources() ++ protoDirs.map(PathRef(_)) }

  override def ivyDeps = T {
    val deps = AkkaGrpcGenerator.suggestedDependencies(
      Scala,
      codeGenerationType(),
      ScalaBinaryVersion(artifactScalaVersion()),
      akkaGrpcVersion(),
      grpcVersion()
    ).map { artifact => ivy"${artifact.groupId}:${artifact.artifactId}:${artifact.version}" }
    super.ivyDeps() ++ Agg(deps:_*)
  }

  private def compileAkkaGrpc: T[PathRef] = T {
    val outDir = T.dest
    AkkaGrpcGenerator.run(
      protoFiles = protoFiles().map(_.path),
      outDir = outDir,
      language = Scala,
      codeGenerationType = codeGenerationType(),
      protocVersion = protocVersion(),
      includeStandardTypes = includeStandardTypes(),
      settings = generatorOptions(),
      protoIncludeDirs = protoIncludeDirs(),
      protocOptions = protocOptions()
    )
    Result.Success(PathRef(outDir))
  }

  private def protoFiles: T[Seq[PathRef]] = T {
    protoDirs.flatMap(os.walk(_, skip = _.ext != "proto")).map(PathRef(_))
  }
}
