package sample.invoice

import com.fajrbahr.mediatork.pipeline.buildin.TransactionProvider

class InvoiceRepository {

    private val store = mutableMapOf<String, Invoice>()

    fun save(invoice: Invoice) {
        store[invoice.id] = invoice
    }

    fun findById(id: String): Invoice? = store[id]
    fun all(): List<Invoice> = store.values.toList()

    val transactionProvider = object : TransactionProvider {
        override suspend fun <T> withTransaction(block: suspend () -> T): T {
            val snapshot = store.toMutableMap()
            return try {
                block()
            } catch (e: Throwable) {
                store.clear()
                store.putAll(snapshot)
                throw e
            }
        }
    }
}
