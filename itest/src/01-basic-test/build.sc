
import mill._
import mill.define._
import mill.scalalib._

import $exec.plugins
import com.github.tjarvstrand.mill._
import com.github.tjarvstrand.mill.CodeGenerationType.Client

object core extends AkkaGrpcScalaModule {
  def scalaVersion = "2.13.2"
  def codeGenerationType = Client
  def mainClass = Some("com.example.Main")
}

def verify(): Command[Unit] = T.command {
  core.compile()
  ()
}
