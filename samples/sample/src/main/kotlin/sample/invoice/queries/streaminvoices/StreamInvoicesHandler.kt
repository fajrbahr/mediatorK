package sample.invoice.queries.streaminvoices

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.StreamRequestHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository

class StreamInvoicesHandler(
    private val repo: InvoiceRepository,
) : StreamRequestHandler<StreamInvoicesQuery, Invoice> {
    override fun handle(mediator: Mediator, requestContext: RequestContext, request: StreamInvoicesQuery): Flow<Invoice> =
        repo.all().asFlow().let { flow ->
            if (request.status != null) flow.filter { it.status == request.status } else flow
        }
}
