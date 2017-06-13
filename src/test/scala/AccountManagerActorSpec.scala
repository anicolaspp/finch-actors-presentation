package nico.bank.demo.test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import nico.bank.demo.bank.Manager.{Accounts, AccountsResponse, Get, Put}
import nico.bank.demo.bank.{Account, Manager}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

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

  override protected def afterAll(): Unit = system.terminate()
}
