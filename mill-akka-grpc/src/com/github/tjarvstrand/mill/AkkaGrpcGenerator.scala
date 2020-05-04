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

import akka.grpc.gen.CodeGenerator
import akka.grpc.gen.CodeGenerator.ScalaBinaryVersion
import akka.grpc.gen.ProtocSettings
import akka.grpc.gen.StdoutLogger
import akka.grpc.gen.javadsl.JavaClientCodeGenerator
import akka.grpc.gen.javadsl.JavaInterfaceCodeGenerator
import akka.grpc.gen.javadsl.JavaServerCodeGenerator
import akka.grpc.gen.scaladsl.ScalaClientCodeGenerator
import akka.grpc.gen.scaladsl.ScalaServerCodeGenerator
import akka.grpc.gen.scaladsl.ScalaTraitCodeGenerator
import com.github.os72.protocjar.Protoc
import com.github.tjarvstrand.mill.CodeGenerationType.Client
import com.github.tjarvstrand.mill.CodeGenerationType.Server
import com.github.tjarvstrand.mill.Language.Java
import com.github.tjarvstrand.mill.Language.Scala
import coursier.MavenRepository
import os.Path
import protocbridge.Artifact
import protocbridge.Generator
import protocbridge.JvmGenerator
import protocbridge.ProtocBridge
import protocbridge.Target
import protocbridge.frontend.PluginFrontend
import scalapb.ScalaPbCodeGenerator

sealed trait Language
object Language {
  case object Scala extends Language
  case object Java extends Language
}

sealed trait CodeGenerationType
object CodeGenerationType {
  case object Client extends CodeGenerationType
  case object Server extends CodeGenerationType
}

trait GeneratorConfig {
  def codeGenerationType: CodeGenerationType
  def settings: Seq[String]
  def generators: Seq[Generator]

  def suggestedDependencies(scalaBinaryVersion: ScalaBinaryVersion): Seq[Artifact] = {
    generators.flatMap(_.suggestedDependencies).distinct.map { artifact =>
      artifact.copy(artifactId = artifact.artifactId.replaceAll("_2.1[0-9]$", "") + "_" + scalaBinaryVersion.prefix)
    }
  }
}

object GeneratorConfig {
  def apply(language: Language,
            codeGenerationType: CodeGenerationType,
            settings: Seq[String] = Seq.empty): GeneratorConfig = {
    language match {
      case Java => new JavaGeneratorConfig(codeGenerationType, settings)
      case Scala => new ScalaGeneratorConfig(codeGenerationType, settings)
    }
  }

  def convertGenerator(codeGenerator: CodeGenerator) : protocbridge.Generator = {
    JvmGenerator(codeGenerator.name, codeGenerator.run(_, StdoutLogger))
  }
}

class ScalaGeneratorConfig(val codeGenerationType: CodeGenerationType,
                           settings_ : Seq[String] = Seq.empty) extends GeneratorConfig {
  val settings: Seq[String] = (settings_ :+ "flat_package").distinct.intersect(ProtocSettings.scalapb)

  val generators: Seq[Generator] = {
    val glueGenerators = codeGenerationType match {
      case Client => Seq(ScalaTraitCodeGenerator, ScalaClientCodeGenerator)
      case Server => Seq(ScalaTraitCodeGenerator, ScalaServerCodeGenerator)
    }

    JvmGenerator("scala", ScalaPbCodeGenerator) +: glueGenerators.map(GeneratorConfig.convertGenerator)
  }
}

class JavaGeneratorConfig(val codeGenerationType: CodeGenerationType,
                          settings_ : Seq[String] = Seq.empty) extends GeneratorConfig {
  val settings: Seq[String] = settings_.intersect(ProtocSettings.protocJava)

  val generators: Seq[Generator] = {
    val glueGenerators = codeGenerationType match {
      case Client => Seq(JavaInterfaceCodeGenerator, JavaClientCodeGenerator)
      case Server => Seq(JavaInterfaceCodeGenerator, JavaServerCodeGenerator)
    }

    protocbridge.gens.java +: glueGenerators.map(GeneratorConfig.convertGenerator)
  }
}

object AkkaGrpcGenerator {

  def suggestedDependencies(language: Language,
                            codeGenerationType: CodeGenerationType,
                            scalaBinaryVersion: ScalaBinaryVersion,
                            akkaGrpcVersion: String,
                            grpcVersion: String): Seq[Artifact] = {
    val scalaDeps = GeneratorConfig(language, codeGenerationType).suggestedDependencies(scalaBinaryVersion)

    val akkaGrpcRuntime = Artifact(
      "com.lightbend.akka.grpc", s"akka-grpc-runtime_${scalaBinaryVersion.prefix}", akkaGrpcVersion
    )

    val stub = codeGenerationType match {
      case Client => Seq(Artifact("io.grpc", "grpc-stub", grpcVersion))
      case Server => Seq.empty
    }

    akkaGrpcRuntime +: (scalaDeps ++ stub)
  }

  def run(protoDirs: Seq[Path],
          outDir: Path,
          language: Language,
          codeGenerationType: CodeGenerationType,
          protocVersion: String,
          includeStandardTypes: Boolean = true,
          settings: Seq[String] = Seq.empty,
          protoIncludeDirs: Seq[Path] = Seq.empty,
          extraGenerators: Seq[CodeGenerator] = Seq.empty,
          protocOptions: Seq[String] = Seq.empty): Unit = {
    val config = GeneratorConfig(language, codeGenerationType, settings)
    val extraGenerators_ = extraGenerators.map(GeneratorConfig.convertGenerator)
    os.makeDir.all(outDir)
    protoDirs.foreach { protoDir =>
      val targets = (config.generators ++ extraGenerators_).map { Target(_, outDir.toIO, config.settings) }
      val files = os.walk(protoDir, skip = _.ext != "proto").toSet
      println(s"Compiling ${files.size} protobuf files to $outDir")
      runProtocBridge(protocVersion, files, protoDir +: protoIncludeDirs, protocOptions, targets, includeStandardTypes)
    }
  }

  def runProtocBridge(protocVersion: String,
                      protoFiles: Set[Path],
                      protoIncludes: Seq[Path],
                      protocOptions: Seq[String],
                      targets: Seq[Target],
                      includeStandardTypes: Boolean): Unit =
    try {
      val includeFiles = protoIncludes.map("-I" + _)
      val includeStandardTypesSetting = if(includeStandardTypes) Seq("--include_std_types") else Seq.empty

      val exitCode = ProtocBridge.run(
        args => Protoc.runProtoc(s"-v$protocVersion" +: args.toArray),
        targets,
        includeFiles ++ protocOptions ++ includeStandardTypesSetting ++ protoFiles.map(_.toString),
        pluginFrontend = PluginFrontend.newInstance)
      if(exitCode != 0) {
        throw new RuntimeException(s"protoc exit code $exitCode")
      }
    } catch {
      case e: Exception =>
        throw new RuntimeException("error occurred while compiling protobuf files: %s".format(e.getMessage), e)
    }
}

