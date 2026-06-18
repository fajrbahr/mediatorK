package sample.invoice.queries.streaminvoices

import com.fajrbahr.mediatork.StreamRequest
import sample.invoice.Invoice
import sample.invoice.InvoiceStatus

data class StreamInvoicesQuery(val status: InvoiceStatus? = null) : StreamRequest<Invoice>
