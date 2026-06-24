package sample.invoice.queries.streaminvoices

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository
import sample.invoice.InvoiceStatus

data class StreamInvoicesQuery(val status: InvoiceStatus? = null) : StreamRequest<Invoice>

class StreamInvoicesHandler(
    private val repo: InvoiceRepository,
) : StreamRequestHandler<StreamInvoicesQuery, Invoice> {
    override fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: StreamInvoicesQuery,
    ): Flow<Invoice> {
        val list = if (request.status != null) repo.allByStatus(request.status) else repo.all()
        return list.asFlow()
    }
}
