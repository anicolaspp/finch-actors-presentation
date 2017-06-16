package nico.bank.demo.bank

import java.io.{File, FileOutputStream, OutputStream}

import scala.util.{Failure, Random, Success, Try}

trait Journal {

  def transactions(): List[Transaction]

  def account: Account

  def add(transaction: Transaction): Account
}

object Journal {

  def For(accountId: String): Journal = new Journal {

    val = st = scala.collection.mutable.MutableList.empty[Transaction]

    override def transactions(): List[Transaction] = st.toList

    override def account: Account = Account(accountId, transactions.map(_.balance).sum)

    override def add(transaction: Transaction): Account = (transaction.accountId, account.balance + transaction.balance >= 0) match {
      case (`accountId`, true)  =>  st += transaction; account
      case _                    =>  account
    }
  }
}

trait Persister[A] {
  def store(value: A): Boolean

  def where: String
}

object Persister {

  def instance[A](func: A => Boolean, w: String) = new Persister[A] {
    override def store(value: A): Boolean = func(value)

    override def where: String = w
  }

  implicit val inMemoryIntPersister = new Persister[Int] {
    override def store(value: Int): Boolean = true

    override def where: String = "memory"
  }
}



