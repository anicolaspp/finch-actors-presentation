package nico.bank.demo

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import cats.Functor
import com.twitter
import com.twitter.util.{Return, Throw}
import io.finch.Endpoint
import nico.bank.demo.TransactionEndpoint.GetMoneyResult
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import nico.bank.demo.Manager.AccountsResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//
//import akka.actor.ActorRef
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
//import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.server.Route
//import akka.util.Timeout
//import nico.bank.demo.Manager.AccountsResponse
//import nico.bank.demo.TransactionEndpoint.CreditTransaction
//import spray.json._
//
//import scala.concurrent.ExecutionContext
//
//trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
//  implicit val transactionFormat = jsonFormat2(CreditTransaction)
//  implicit val accountFormat = jsonFormat2(Account)
//}
//
//trait TransactionEndpoint extends JsonSupport {
//
//  def credit: Route
//
//  def account: Route
//
//  def endpoint: Route = credit ~ account
//}
//
//import akka.pattern._
//import scala.concurrent.duration._
//object TransactionEndpoint {
//
//  def apply(manager: ActorRef)(implicit ec: ExecutionContext): TransactionEndpoint = new TransactionEndpoint {
//
//    implicit val askTimeout: Timeout = 3 seconds
//
//    override def credit = pathPrefix("credit") {
//      post {
//        entity(as[CreditTransaction]) { credit =>
//
//          complete(
//            (manager ? Manager.Put(credit.account, credit.amount)).mapTo[Account]
//          )
//
//        }
//      }
//    }
//
//    override def account = get {
//      parameters("acc") { account =>
//        complete(
//          (manager ? Manager.Accounts).mapTo[AccountsResponse].map { response =>
//            response.accounts.find(_.id == account).fold[Account](AccountUtils.empty)(identity)
//          }
//        )
//      }
//    }
//  }
//
//
//
//
//  case class CreditTransaction(account: String, amount: Int)
//}

import akka.pattern.ask
import scala.concurrent.duration._
import com.twitter.util.{Future => TFuture, Promise => TPromise, Return, Throw}
import scala.concurrent.{Future => SFuture, Promise => SPromise, ExecutionContext}
import scala.util.{Success, Failure}

trait TransactionEndpoint {

  def putMoney: Endpoint[Account]

  def getMoney: Endpoint[GetMoneyResult]

  def account: Endpoint[Account]

  def accounts: Endpoint[List[Account]]
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

    override def getMoney: Endpoint[GetMoneyResult] = ???

    override def accounts: Endpoint[List[Account]] = ???

    override def account: Endpoint[Account] = get("account" :: string) { accountId: String =>

      (manager ? Manager.Accounts).mapTo[AccountsResponse].map { response =>
        response.accounts.find(_.id == accountId)
      }
        .asTwitter
        .map {
          case Some(acc)  =>  Ok(acc)
          case None       =>  Ok(AccountUtils.empty)
        }
    }
  }

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


//object Implicits {
//  implicit def scalaToTwitterFuture[T](f: Future[T])(implicit ec: ExecutionContext): com.twitter.Future[T] = {
//    val promise = twitter.Promise[T]()
//    f.onComplete(promise update _)
//    promise
//  }
//}