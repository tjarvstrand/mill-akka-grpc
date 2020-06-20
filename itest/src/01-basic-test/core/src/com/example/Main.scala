package com.example

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import art.ArticleServiceClient

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("01-basic-test")
    val service: GrpcClientSettings = GrpcClientSettings.connectToServiceAt("localhost", 1234)(system)
    ArticleServiceClient(service)(system)
  }
}