
package nico.bank.demo

import akka.actor.{Actor, ActorRef, Props}
import nico.bank.demo.AccountActor._
import nico.bank.demo.Manager._

import scala.concurrent.Future
import scala.util.{Random, Success}


case class Account(id: String, balance: Int)

case class Transaction(id: String, accountId: String, balance: Int)


trait Journal {

  def transactions(): List[Transaction]

  def account: Account

  def add(transaction: Transaction): Account
}


object Journal {

  def For(accountId: String): Journal = new Journal {

    var st = scala.collection.mutable.MutableList.empty[Transaction]

    override def transactions(): List[Transaction] = st.toList

    override def account: Account = Account(accountId, transactions.toList.map(_.balance).sum)

    override def add(transaction: Transaction): Account = {
      if (transaction.accountId == accountId) {



        st += transaction

        if (account.balance < 0){

          st = st.dropRight(1)

          account
        } else account
      } else account
    }
  }
}


class AccountActor(accountId: String) extends Actor {

  val journal = Journal.For(accountId)

  override def receive: Receive = {
    case GetBalance           =>  sender() ! journal.account
    case AddBalance(balance)  =>  {
      journal.add(Transaction(transactionId, accountId, balance))

      sender() ! BalanceAdded
    }
    case RemoveBalance(balance) =>  {

      val b = journal.account.balance

      val st = journal.add(Transaction(transactionId, accountId, balance * -1))

      if (b == st.balance) {
        sender() ! InsufficientFoundsFor(balance)
      } else {
        sender() ! BalanceRemoved
      }
    }
  }

  private def transactionId = Random.nextLong().toString
}

object AccountActor {

  def props(accountId: String): Props = Props(new AccountActor(accountId))

  case object GetBalance
  case object BalanceAdded
  case class AddBalance(balance: Int)

  case class RemoveBalance(balance: Int)
  case class InsufficientFoundsFor(balance: Int)

  case object BalanceRemoved
}

class Manager(accounts: List[String]) extends Actor {
  import scala.concurrent.duration._
  import akka.pattern._
  import context.dispatcher
  import akka.pattern.pipe
  implicit val timeout = akka.util.Timeout(5 seconds)

  accounts.foreach(id => context.actorOf(AccountActor.props(id), id))

  override def receive: Receive = {
    case Accounts => getAccounts(context.children.toList, List.empty).map(xs => AccountsResponse(xs)).pipeTo(sender())

    case Put(acc, amount) => {
      context.child(acc).fold(sender() ! PutFailed){ actor =>
        actor ! AddBalance(amount)

        (actor ? GetBalance).mapTo[Account].pipeTo(sender())
      }
    }

    case Get(acc, amount) => {
      context.child(acc).fold(sender() ! GetFailed){ actor =>
        actor ! RemoveBalance(amount)

        (actor ? GetBalance).mapTo[Account].pipeTo(sender())
      }

    }
  }

  def getAccounts(accounts: List[ActorRef], result: List[Account]): Future[List[Account]] = accounts match {
    case Nil    =>  Future(result)
    case h :: t =>  (h ? GetBalance).mapTo[Account].flatMap { r => getAccounts(t, r :: result) }
  }
}

object Manager {
  def props(accounts: List[String]): Props = Props(new Manager(accounts))

  case class AccountsResponse(accounts: List[Account])
  case object Accounts

  case class Put(accountId: String, amount: Int)
  case object PutFailed
  
  case class Get(accountId: String, amount: Int)
  case object GetFailed
}

