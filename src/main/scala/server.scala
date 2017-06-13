package nico.bank.demo.server



import com.twitter.finagle.Http
import com.twitter.util.{Await, Future}
import io.finch._
import io.circe.generic.auto._
import io.finch.circe._



object server {

  def main(args: Array[String]): Unit = {

    val hello: Endpoint[String] = get("hello") {
      Future { Ok("hello") }
    }

    val service = hello.toService

    Await.ready(Http.server.serve(":8080", service))

//
//    implicit val system = ActorSystem("BankServer")
//    implicit val mat = ActorMaterializer()
//    implicit val ec = system.dispatcher
//
//    val endpoint = TransactionEndpoint(system.actorOf(Manager.props(List("001", "002")))).endpoint
//
//    val s = path("hello") {
//      parameters('color) { (color) =>
//        complete(s"The color is '$color''")
//      }
//    }
//
//    val bindingFuture = Http().bindAndHandle(s, "localhost", 8080)
//
//    scala.io.StdIn.readLine()
//
//    bindingFuture
//      .flatMap(_.unbind())
//      .onComplete(_ => system.terminate())
  }

}
