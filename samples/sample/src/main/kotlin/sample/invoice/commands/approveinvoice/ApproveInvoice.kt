package sample.invoice.commands.approveinvoice

import com.fajrbahr.mediatork.api.*
import sample.invoice.InvoiceRepository
import sample.invoice.InvoiceStatus

data class ApproveInvoiceCommand(val id: String) : Request<Unit>

class ApproveInvoiceHandler(
    private val repo: InvoiceRepository,
) : RequestHandler<ApproveInvoiceCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: ApproveInvoiceCommand,
    ) {
        val invoice = repo.findById(request.id)
            ?: error("Invoice '${request.id}' not found")
        check(invoice.status == InvoiceStatus.PENDING) {
            "Invoice '${request.id}' is already ${invoice.status}"
        }
        repo.save(invoice.copy(status = InvoiceStatus.APPROVED))
    }
}
