package nico.bank.demo.endpoint

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.twitter.util.{Future => TFuture, Promise => TPromise}
import io.circe.generic.auto._
import io.finch.circe._
import io.finch.{Endpoint, _}
import nico.bank.demo.{Account, AccountUtils, Manager}
import nico.bank.demo.Manager.AccountsResponse
import nico.bank.demo.endpoint.TransactionEndpoint.GetMoneyResult

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future => SFuture, Promise => SPromise}
import scala.util.{Failure, Success}

trait TransactionEndpoint {

  def putMoney: Endpoint[Account]

  def getMoney: Endpoint[GetMoneyResult]

  def account: Endpoint[Account]

  def accounts: Endpoint[List[Account]]

//  def api = putMoney :+: getMoney :+: account :+: accounts
}

object TransactionEndpoint {

  def apply(manager: ActorRef)(implicit ec: ExecutionContext): TransactionEndpoint = new TransactionEndpoint {
    implicit val askTimeout: Timeout = 3 seconds

    override def putMoney: Endpoint[Account] = post("credit" :: jsonBody[PutMoney]) { putRquest: PutMoney =>

      (manager ? Manager.Put(putRquest.acc, putRquest.amount))
        .mapTo[Account]
        .asTwitter
        .map(Ok)

    }

    override def getMoney: Endpoint[GetMoneyResult] = post("account" :: "money" :: string :: int) {
      (accountId: String, amount: Int) =>
        
        getAccount(manager, accountId)
          .flatMap[Output[GetMoneyResult]] {
          case None => SFuture[Output[GetMoneyResult]](NotFound(new Exception()))
          case Some(acc) => {
            procesGetMoney(acc, amount, manager).map { newAccount =>
              if (newAccount.balance == acc.balance) Ok(GetMoneyResult(amount, false)) else Ok(GetMoneyResult(amount, true))
            }
          }
        }.asTwitter
    }





//        getAccount(manager, accountId)
//          .asTwitter
//          .map[Output[GetMoneyResult]] {
//          case None => NotFound(new Exception())
//          case Some(acc) => procesGetMoney(acc, amount, manager).flatMap { newAccount =>
//            if (newAccount.balance == acc) GetMoneyResult(amount, false) else GetMoneyResult(amount, true)
//          }.asTwitter
//              .map(Ok)
//        }
//    }

    private def procesGetMoney(account: Account, amount: Int, manager: ActorRef) =
      (manager ? Manager.Get(account.id, amount))
        .mapTo[Account]

    override def accounts: Endpoint[List[Account]] = ???

    override def account: Endpoint[Account] = get("account" :: string) { accountId: String =>

      getAccount(manager, accountId)
        .asTwitter
        .map {
          case Some(acc) => Ok(acc)
          case None => Ok(AccountUtils.empty)
        }
    }

    private def getAccount(manager: ActorRef, accountId: String) =
      (manager ? Manager.Accounts)
        .mapTo[AccountsResponse]
        .map { response => response.accounts.find(_.id == accountId)
        }
  }

//      .map {
//        case Some(acc) => Ok(acc)
//        case None => Ok(AccountUtils.empty)
//      }


  case class GetMoneyResult(requested: Int, processed: Boolean)

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