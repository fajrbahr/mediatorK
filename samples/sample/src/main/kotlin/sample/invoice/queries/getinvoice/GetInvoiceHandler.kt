package sample.invoice.queries.getinvoice

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository

class GetInvoiceHandler(
    private val repo: InvoiceRepository,
) : RequestHandler<GetInvoiceQuery, Invoice> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: GetInvoiceQuery): Invoice =
        repo.findById(request.id) ?: error("Invoice ${request.id} not found")
}
