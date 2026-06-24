package sample.invoice

import com.fajrbahr.mediatork.pipeline.buildin.TransactionProvider

/**
 * In-memory invoice store with lightweight snapshot-based transaction support.
 *
 * Each [begin] takes a full snapshot of current state. [commit] discards the snapshot.
 * [rollback] restores the snapshot, undoing any mutations that happened since [begin].
 */
class InvoiceRepository {

    private val invoices = mutableMapOf<String, Invoice>()
    private var snapshot: Map<String, Invoice>? = null

    val transactionProvider = object : TransactionProvider {
        override suspend fun begin() {
            snapshot = invoices.toMap()
        }

        override suspend fun commit() {
            snapshot = null
        }

        override suspend fun rollback() {
            snapshot?.let {
                invoices.clear()
                invoices.putAll(it)
                snapshot = null
            }
        }
    }

    fun save(invoice: Invoice) {
        invoices[invoice.id] = invoice
    }

    fun findById(id: String): Invoice? = invoices[id]

    fun all(): List<Invoice> = invoices.values.toList()

    fun allByStatus(status: InvoiceStatus): List<Invoice> =
        invoices.values.filter { it.status == status }
}
