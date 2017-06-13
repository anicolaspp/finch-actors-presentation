package nico.bank.demo.server

import akka.actor.ActorSystem
import com.twitter.finagle.Http
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch.circe._
import nico.bank.demo.bank.Manager
import nico.bank.demo.endpoint.TransactionEndpoint

object server {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("app")


    val manager = system.actorOf(Manager.props(List("001", "002")))

    val service = TransactionEndpoint(manager)(system.dispatcher).api.toService

    Await.ready(Http.server.serve(":8080", service))
  }
}
