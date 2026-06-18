package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.validator.ValidationException
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository

class CreateInvoiceHandler(
    private val repo: InvoiceRepository,
    private val domainValidator: CreateInvoiceDomainValidator,
    private val persistenceValidator: CreateInvoicePersistenceValidator,
) : RequestHandler<CreateInvoiceCommand, Unit> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateInvoiceCommand) {
        val domainResult = domainValidator.validate(request)
        if (!domainResult.isValid) throw ValidationException(domainResult.errors)

        val invoice = Invoice(id = request.id, amount = request.amount)

        val persistenceResult = persistenceValidator.validate(request)
        if (!persistenceResult.isValid) throw ValidationException(persistenceResult.errors)

        repo.save(invoice)
    }
}
