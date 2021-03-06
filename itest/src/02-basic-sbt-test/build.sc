
import mill._
import mill.define._
import mill.scalalib._

import $exec.plugins
import com.github.tjarvstrand.mill._
import com.github.tjarvstrand.mill.CodeGenerationType.Client

object core extends AkkaGrpcScalaModule with SbtModule {
  def scalaVersion = "2.13.2"
  def codeGenerationType = Client
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.typesafe.akka::akka-actor:2.6.6"
  )
}

def verify(): Command[Unit] = T.command {
  core.compile()
  ()
}
