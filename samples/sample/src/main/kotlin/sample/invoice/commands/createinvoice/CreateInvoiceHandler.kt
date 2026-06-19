package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository

class CreateInvoiceHandler(
    private val repo: InvoiceRepository,
) : RequestHandler<CreateInvoiceCommand, Unit> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateInvoiceCommand) {
        repo.save(Invoice(id = request.id, amount = request.amount))
    }
}
