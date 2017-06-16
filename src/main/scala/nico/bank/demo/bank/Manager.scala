package nico.bank.demo.bank

import akka.actor.{Actor, ActorRef, Props}
import nico.bank.demo.bank.AccountActor.{AddBalance, GetBalance, RemoveBalance}
import nico.bank.demo.bank.Manager._

import scala.concurrent.Future

class Manager(accounts: List[String]) extends Actor {
  import akka.pattern.{pipe, _}
  import context.dispatcher

  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(5 seconds)

  accounts.foreach(id => context.actorOf(AccountActor.props(id), id))

  override def receive: Receive = {
    case Accounts => getAccounts(context.children.toList, List.empty).map(AccountsResponse).pipeTo(sender())

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