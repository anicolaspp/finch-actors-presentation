package nico.bank.demo.endpoint

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.twitter.util.{Future => TFuture, Promise => TPromise}
import io.circe.generic.auto._
import io.finch.circe._
import io.finch.{Endpoint, _}
import nico.bank.demo.bank.Manager.AccountsResponse
import nico.bank.demo.bank.{Account, AccountUtils, Manager}
import nico.bank.demo.endpoint.TransactionEndpoint.Response

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future => SFuture, Promise => SPromise}
import scala.util.{Failure, Success}

trait TransactionEndpoint {

  def putMoney: Endpoint[Account]

  def getMoney: Endpoint[Response]

  def account: Endpoint[Account]

  def accounts: Endpoint[List[Account]]

  def api = putMoney :+: getMoney :+: account :+: accounts
}

object TransactionEndpoint {

  def apply(manager: ActorRef)(implicit ec: ExecutionContext): TransactionEndpoint = new TransactionEndpoint {
    implicit val askTimeout: Timeout = 3 seconds

    override def putMoney: Endpoint[Account] = post("credit" :: jsonBody[PutMoney]) { putRquest: PutMoney =>
      (manager ? Manager.Put(putRquest.acc, putRquest.amount))
        .mapTo[Account]
        .map(Ok)
        .asTwitter
    }

    override def getMoney: Endpoint[Response] = get("account" :: "money" :: string :: int) { (accountId: String, amount: Int) =>
      getAccount(manager, accountId)
        .flatMap[Response] {
        case None       =>  SFuture { AccountNotFound(accountId) }
        case Some(acc)  =>  {
          procesGetMoney(acc, amount, manager).map { newAccount =>
            if (newAccount.balance == acc.balance) GetMoneyResult(amount, false) else GetMoneyResult(amount, true)
          }
        }
      }
        .map(Ok)
        .asTwitter
    }
    
    override def accounts: Endpoint[List[Account]] = get("accounts") {
      Ok(List.empty[Account])
    }

    override def account: Endpoint[Account] = get("account" :: string) { accountId: String =>
      getAccount(manager, accountId)
        .map[Account] {
          case Some(acc)  =>  acc
          case None       =>  AccountUtils.empty
        }
        .map(Ok)
        .asTwitter
    }

    private def getAccount(manager: ActorRef, accountId: String) =
      (manager ? Manager.Accounts)
        .mapTo[AccountsResponse]
        .map(_.accounts.find(_.id == accountId))

    private def procesGetMoney(account: Account, amount: Int, manager: ActorRef) =
      (manager ? Manager.Get(account.id, amount))
        .mapTo[Account]
  }

  sealed trait Response

  case class GetMoneyResult(requested: Int, processed: Boolean) extends Response

  case class AccountNotFound(accountId: String) extends Response

  case class PutMoney(acc: String, amount: Int)

  implicit class RichSFuture[A](val f: SFuture[A]) extends AnyVal {
    def asTwitter(implicit e: ExecutionContext): TFuture[A] = {
      val p: TPromise[A] = new TPromise[A]
      f.onComplete {
        case Success(value) => p.setValue(value)
        case Failure(exception) => p.setException(exception)
      }

      p
    }
  }
}