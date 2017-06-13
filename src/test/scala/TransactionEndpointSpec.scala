import akka.actor.ActorSystem
import akka.testkit.TestKit

import nico.bank.demo._
import org.scalatest.{FlatSpecLike, Matchers}
import io.finch._, io.circe._
import com.twitter.io.Buf

import scala.concurrent.ExecutionContext.Implicits.global

class TransactionEndpointSpec extends TestKit(ActorSystem("TransactionEndpointSpec"))
  with FlatSpecLike
  with Matchers {
  
  it should "match post" in {

    val manager = system.actorOf(Manager.props(List("001")))

    val putMoney = TransactionEndpoint(manager).putMoney

    val input = Input.post("/credit")
      .withBody[Application.Json](Buf.Utf8("""{"acc":"001","amount":5}"""))

     putMoney(input).awaitValueUnsafe() should be (Some(Account("001", 5)))

//
//    Post("/credit", CreditTransaction("001", 5)) ~> endpoint.credit ~> check {
//      responseAs[Account] should be (Account("001", 5))
//    }

    val otherInput = Input.post("/credit")
      .withBody[Application.Json](Buf.Utf8("""{"acc":"001","amount":5}"""))

    putMoney(otherInput).awaitValueUnsafe() should be (Some(Account("001", 10)))

//    Post("/credit", CreditTransaction("001", 5)) ~> endpoint.credit ~> check {
//      responseAs[Account] should be (Account("001", 10))
//    }
  }
//
  it should "match account" in {
    val manager = system.actorOf(Manager.props(List("001")))

    manager ! Manager.Put("001", 100)

    val account = TransactionEndpoint(manager).account

    val input = Input.get("/account/001")

    account(input).awaitValueUnsafe() should be (Some(Account("001", 100)))
//
//    Get("/account?acc=001") ~> endpoint.account ~> check {
//      responseAs[Account] should be (Account("001", 100))
//    }
  }
//
  it should "match unknown account" in {
    val manager = system.actorOf(Manager.props(List.empty))

    val account = TransactionEndpoint(manager).account

    val input = Input.get("/account/001")

    account(input).awaitValueUnsafe() should be (Some(AccountUtils.empty))

//    Get("/account?acc=001") ~> endpoint.account ~> check {
//      responseAs[Account] should be (AccountUtils.empty)
//    }
  }
}
