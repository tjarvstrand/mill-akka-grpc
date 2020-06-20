[![Build Status](https://travis-ci.com/tjarvstrand/mill-akka-grpc.svg?branch=master)](https://travis-ci.com/tjarvstrand/mill-akka-grpc)
![Latest Version](https://img.shields.io/github/v/tag/tjarvstrand/mill-akka-grpc?include_prereleases&sort=semver)

# mill-akka-grpc

A module to build akka-grpc modules in mill. Requires:

  * akka >= 2.6.6
  * mill >= 0.7.0

## Limitations

* Only works for scala right now

## Get started

`build.sc`:
```
import mill._, mill.scalalib._

import $ivy.`com.github.tjarvstrand::mill-akka-grpc:0.1.0`
import com.github.tjarvstrand.mill._

object app extends AkkaGrpcScalaModule {
  def scalaVersion = "2.13.2"
  def codeGenerationType = CodeGenerationType.Client
}
```

See `AkkaGrcpScalaModule` for additional options
