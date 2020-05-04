
import mill._
import mill.define._
import mill.scalalib._

import $exec.plugins
import com.github.tjarvstrand.mill._
import com.github.tjarvstrand.mill.CodeGenerationType.Client

object core extends AkkaGrpcScalaModule with SbtModule {
  def scalaVersion = "2.12.4"
  def codeGenerationType = Client
}

def verify(): Command[Unit] = T.command {
  core.compile()
  ()
}
