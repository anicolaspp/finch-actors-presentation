package nico.bank.demo.test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import nico.bank.demo.AccountActor._
import nico.bank.demo.Manager.{Accounts, AccountsResponse, Get, Put}
import nico.bank.demo._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, FlatSpecLike, Matchers}

class JournalSpec extends FlatSpec
  with Matchers
  with BeforeAndAfterAll {

  it should "start with no transactions" in {

    val journal = Journal.For("001")

    journal.transactions() should be (List.empty[Transaction])
  }

  it should "return account with not balance" in {

    Journal.For("001").account should be (Account("001", 0))

  }

  it should "add transaction" in {

    val journal = Journal.For("001")

    journal.add(Transaction("1", "001", 5)) should be (Account("001", 5))
    journal.add(Transaction("2", "001", 5)) should be (Account("001", 10))

    journal.transactions() should contain inOrderOnly(Transaction("1", "001", 5), Transaction("2", "001", 5))

    journal.account.balance should be (10)
  }

  it should "not update if wrong account" in {

    val journal = Journal.For("001")
    journal.add(Transaction("1", "001", 5))
    journal.add(Transaction("2", "002", 10)) should be (Account("001", 5))
  }

  it should "remove balance correctly" in {
    val journal = Journal.For("001")

    journal.add(Transaction("1", "001", 5))
    journal.add(Transaction("1", "001", -10)) should be (Account("001", 5))
  }
}


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

class AccountManagerActorSpec extends TestKit(ActorSystem("AccountManagerActorSpec"))
  with FlatSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll {

  it should "start accounts" in {

    val manager = system.actorOf(Manager.props(List("001", "002")))

    manager ! Accounts

    val accountsResponse = expectMsgType[AccountsResponse]

    accountsResponse.accounts.foreach { _.balance should be (0) }
  }

  it should "send puts to account" in {
    val manager = system.actorOf(Manager.props(List("001", "002")))

    manager ! Put("001", 5)
    manager ! Put("001", 10)
    manager ! Put("002", 4)
    manager ! Accounts

    expectMsgType[Account]
    expectMsgType[Account]
    expectMsgType[Account]

    val accountsResponse = expectMsgType[AccountsResponse]

    accountsResponse.accounts should contain only(Account("001", 15), Account("002", 4))
  }

  it should "send gets to account" in {
    val manager = system.actorOf(Manager.props(List("001", "002")))

    manager ! Put("001", 10)
    manager ! Get("001", 5)

    manager ! Put("002", 5)
    manager ! Get("002", 10)
    manager ! Accounts

    expectMsgType[Account]
    expectMsgType[Account]
    expectMsgType[Account]
    expectMsgType[Account]


    val accountsResponse = expectMsgType[AccountsResponse]

    accountsResponse.accounts should contain only (Account("001", 5), Account("002", 5))
  }
}

