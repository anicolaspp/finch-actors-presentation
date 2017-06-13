package nico.bank.demo.bank

trait Journal {

  def transactions(): List[Transaction]

  def account: Account

  def add(transaction: Transaction): Account
}

object Journal {

  def For(accountId: String): Journal = new Journal {

    var st = scala.collection.mutable.MutableList.empty[Transaction]

    override def transactions(): List[Transaction] = st.toList

    override def account: Account = Account(accountId, transactions.map(_.balance).sum)

    override def add(transaction: Transaction): Account =
      if (transaction.accountId == accountId) {
        st += transaction

        if (account.balance < 0) {

          st = st.dropRight(1)

          account
        } else account
      } else account
  }
}