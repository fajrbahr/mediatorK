package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules
import com.fajrbahr.mediatork.validator.throwIfInvalid
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository
import kotlin.reflect.KClass

class CreateInvoiceHandler(
    private val repo: InvoiceRepository,
    private val domainValidator: CreateInvoiceDomainValidator,
    private val persistenceValidator: CreateInvoicePersistenceValidator,
) : RequestHandler<CreateInvoiceCommand, Unit> {

    override fun validators() = listOf(CreateInvoiceRequestValidator())

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateInvoiceCommand) {
        domainValidator.validate(request).throwIfInvalid()

        val invoice = Invoice(id = request.id, amount = request.amount)

        persistenceValidator.validate(request).throwIfInvalid()

        repo.save(invoice)
    }
}

sealed class CreateInvoiceError(val message: String) {
    data object IdBlank : CreateInvoiceError("Invoice ID is required")
    data object IdInvalidPrefix : CreateInvoiceError("Invoice ID must start with INV-")
    data object AmountNotPositive : CreateInvoiceError("Amount must be positive")
}

class CreateInvoiceRequestValidator : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class

    override fun validate(request: CreateInvoiceCommand) = rules<CreateInvoiceError> {
        check(request.id.isNotBlank()) { CreateInvoiceError.IdBlank }
        check(request.id.startsWith("INV-")) { CreateInvoiceError.IdInvalidPrefix }
        check(request.amount > 0) { CreateInvoiceError.AmountNotPositive }
    }
}
