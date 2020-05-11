[![Build Status](https://travis-ci.com/tjarvstrand/mill-akka-grpc.svg?branch=master)](https://travis-ci.com/tjarvstrand/mill-akka-grpc)
![Latest Version](https://img.shields.io/github/v/tag/tjarvstrand/mill-akka-grpc?include_prereleases&sort=semver)

# mill-akka-grpc
A module to build akka-grpc modules in mill.

## Limitations
* Only works for scala right now
* Only works in mill versions < 0.7 because akka-grpc-codegen has not yet been published for scala 2.13

## Get started

`build.sc`:
```
import mill._, mill.scalalib._

import $ivy.`com.github.tjarvstrand::mill-akka-grpc:0.1.0`
import com.github.tjarvstrand.mill._

object app extends AkkaGrpcScalaModule {
  def scalaVersion = "2.12.4"
  def codeGenerationType = CodeGenerationType.Client
}
```

See `AkkaGrcpScalaModule` for additional options
