package sample.invoice.queries.getinvoice

import com.fajrbahr.mediatork.api.*
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository

data class GetInvoiceQuery(val id: String) : Request<Invoice>

class GetInvoiceHandler(
    private val repo: InvoiceRepository,
) : RequestHandler<GetInvoiceQuery, Invoice> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetInvoiceQuery,
    ): Invoice = repo.findById(request.id)
        ?: error("Invoice '${request.id}' not found")
}
