package nico.bank.demo.test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import nico.bank.demo.AccountActor._
import nico.bank.demo.{Account, AccountActor}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

class AccountActorSpec extends TestKit(ActorSystem("AccountActorSpec"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  it should "get balance" in {

    val account = system.actorOf(AccountActor.props("001"), "account")

    account ! GetBalance

    expectMsg(Account("001", 0))

  }

  it should "add balance" in {

    val account = system.actorOf(AccountActor.props("001"))

    account ! AddBalance(5)

    expectMsg(BalanceAdded)

    account ! GetBalance

    expectMsg(Account("001", 5))
  }

  it should "remove balance" in {

    val account = system.actorOf(AccountActor.props("001"))

    account ! RemoveBalance(5)
    account ! AddBalance(10)
    account ! RemoveBalance(8)
    account ! GetBalance

    expectMsg(InsufficientFoundsFor(5))
    expectMsg(BalanceAdded)
    expectMsg(BalanceRemoved)
    expectMsg(Account("001", 2))
  }

  override protected def afterAll(): Unit = system.terminate()
}
