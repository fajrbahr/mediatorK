package sample.invoice.commands.approveinvoice

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import sample.invoice.InvoiceRepository
import sample.invoice.InvoiceStatus

class ApproveInvoiceHandler(
    private val repo: InvoiceRepository,
) : RequestHandler<ApproveInvoiceCommand, Unit> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: ApproveInvoiceCommand) {
        val invoice = repo.findById(request.id)
            ?: error("Invoice ${request.id} not found")

        check(invoice.status == InvoiceStatus.PENDING) {
            "Invoice ${request.id} cannot be approved — current status: ${invoice.status}"
        }

        repo.save(invoice.copy(status = InvoiceStatus.APPROVED))
    }
}
