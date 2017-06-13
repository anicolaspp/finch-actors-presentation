package nico.bank.demo.test

import nico.bank.demo._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class JournalSpec extends FlatSpec
  with Matchers {

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