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
import mill._
import mill.api.Result
import mill.scalalib._
import os.Path
import upickle.default._

trait AkkaGrpcScalaModule extends ScalaModule {

  def codeGenerationType: T[CodeGenerationType]

  /**
   * @return The directories where the module's protobuf definitions are located
   */
  def protoDirs: T[Seq[Path]] = T {
    Seq(
      millSourcePath / "src" / "main" / "protobuf",
      millSourcePath / "protobuf"
    ).filter(os.exists)
  }

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
   * @return Whether or not to include the standard protof types from Google
   */
  def includeStandardTypes: T[Boolean] = T(true)

  /**
   * @return Additional options to the akka-grpc generators
   */
  def generatorOptions: T[Seq[String]] = T(Seq.empty[String])

  /**
   * @return Version of the akka-grpc runtime to use
   */
  def akkaGrpcVersion: T[String] = T("0.8.4")

  /**
   * @return Version of the grpc library to use
   */
  def grpcVersion: T[String] = T("1.28.1") // fixme this should be taken from the akka-grpc dependency on grpc-core

  override def generatedSources = T { super.generatedSources() :+ compileAkkaGrpc() }

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
      protoDirs = protoDirs(),
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
}
