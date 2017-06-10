
package nico.bank.demo


case class Account(id: String, balance: Int)

case class Transaction(id: String, accountId: String, balance: Int)



trait Journal {

  def transactions(): List[Transaction]

  def account: Account

  def add(transaction: Transaction): Account
}


object Journal {

  def For(accountId: String): Journal = new Journal {

    val st = scala.collection.mutable.MutableList.empty[Transaction]

    override def transactions(): List[Transaction] = st.toList

    override def account: Account = Account(accountId, transactions.map(_.balance).sum)

    override def add(transaction: Transaction): Account = if (transaction.accountId == accountId) {
      st += transaction

      account
    } else account
  }

}