package sample.invoice

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.StreamRequest
import com.fajrbahr.mediatork.pipeline.TransactionProvider

// ── Domain model ──────────────────────────────────────────────────────────────

enum class InvoiceStatus { PENDING, APPROVED, REJECTED }

data class Invoice(
    val id: String,
    val amount: Double,
    val status: InvoiceStatus = InvoiceStatus.PENDING,
)

// ── Requests ──────────────────────────────────────────────────────────────────

data class CreateInvoiceCommand(val id: String, val amount: Double) : Request.Unit
data class ApproveInvoiceCommand(val id: String) : Request.Unit
data class GetInvoiceQuery(val id: String) : Request<Invoice>
// Stream request — emits invoices one-by-one without loading all into memory
data class StreamInvoicesQuery(val status: InvoiceStatus? = null) : StreamRequest<Invoice>

// ── In-memory repository with manual transaction support ──────────────────────

class InvoiceRepository {

    private val store = mutableMapOf<String, Invoice>()

    fun save(invoice: Invoice) { store[invoice.id] = invoice }
    fun findById(id: String): Invoice? = store[id]
    fun all(): List<Invoice> = store.values.toList()

    /**
     * Snapshots the current store before [block], commits on success, restores on exception.
     * This is the adapter that plugs into [TransactionProvider].
     */
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
