import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.twitter.io.Buf
import io.finch._
import nico.bank.demo.bank.{Account, AccountUtils, Manager}
import nico.bank.demo.endpoint.TransactionEndpoint
import nico.bank.demo.endpoint.TransactionEndpoint.{AccountNotFound, GetMoneyResult}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class TransactionEndpointSpec extends TestKit(ActorSystem("TransactionEndpointSpec"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll {
  
  it should "match post" in {

    val manager = system.actorOf(Manager.props(List("001")))

    val putMoney = TransactionEndpoint(manager).putMoney

    val input = Input.post("/credit")
      .withBody[Application.Json](Buf.Utf8("""{"acc":"001","amount":5}"""))

     putMoney(input).awaitValueUnsafe() should be (Some(Account("001", 5)))

    val otherInput = Input.post("/credit")
      .withBody[Application.Json](Buf.Utf8("""{"acc":"001","amount":5}"""))

    putMoney(otherInput).awaitValueUnsafe() should be (Some(Account("001", 10)))
  }

  it should "match account" in {
    val manager = system.actorOf(Manager.props(List("001")))

    manager ! Manager.Put("001", 100)

    val account = TransactionEndpoint(manager).account

    val input = Input.get("/account/001")

    account(input).awaitValueUnsafe() should be (Some(Account("001", 100)))
  }

  it should "match unknown account" in {
    val manager = system.actorOf(Manager.props(List.empty))

    val account = TransactionEndpoint(manager).account

    val input = Input.get("/account/001")

    account(input).awaitValueUnsafe() should be (Some(AccountUtils.empty))
  }
  
  it should "match get money" in {
    val manager = system.actorOf(Manager.props(List("001")))

    manager ! Manager.Put("001", 100)

    TransactionEndpoint(manager)
      .getMoney(Input.get("/account/money/001/5"))
      .awaitValueUnsafe() should be (Some(GetMoneyResult(5, true)))

    TransactionEndpoint(manager)
      .getMoney(Input.get("/account/money/001/100"))
      .awaitValueUnsafe() should be (Some(GetMoneyResult(100, false)))

    val notFound = TransactionEndpoint(manager)
      .getMoney(Input.get("/account/money/002/100"))
      .awaitValueUnsafe() should be (Some(AccountNotFound("002")))

  }
  
  it should "match accounts" in {

  }

  override protected def afterAll(): Unit = system.terminate()
}
