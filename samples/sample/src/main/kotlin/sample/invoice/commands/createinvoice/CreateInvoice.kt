package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.validator.*
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository

data class CreateInvoiceCommand(val id: String, val amount: Double) : Request<Unit>

sealed class CreateInvoiceError {
    data object IdInvalidPrefix : CreateInvoiceError()
    data object DuplicateId : CreateInvoiceError()
    data object NegativeAmount : CreateInvoiceError()
}

/** REQUEST scope — rejects IDs that don't match the "INV-<value>" pattern. */
class CreateInvoicePersistenceValidator(
    @Suppress("UNUSED_PARAMETER") private val repo: InvoiceRepository,
) : RequestValidator<CreateInvoiceCommand> {
    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        rulesFailFast<CreateInvoiceError> {
            check(request.id.startsWith("INV-") && request.id.length > 4) {
                CreateInvoiceError.IdInvalidPrefix
            }
        }
}

/** DOMAIN scope — rejects duplicate IDs and negative amounts. */
class CreateInvoiceDomainValidator(
    private val repo: InvoiceRepository,
) : RequestValidator<CreateInvoiceCommand> {
    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        rules<CreateInvoiceError> {
            check(repo.findById(request.id) == null) { CreateInvoiceError.DuplicateId }
            check(request.amount >= 0) { CreateInvoiceError.NegativeAmount }
        }
}

class CreateInvoiceHandler(
    private val repo: InvoiceRepository,
    private val domainValidator: CreateInvoiceDomainValidator,
    private val persistenceValidator: CreateInvoicePersistenceValidator,
) : RequestHandler<CreateInvoiceCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateInvoiceCommand,
    ) {
        persistenceValidator.validate(request).throwIfInvalid()
        domainValidator.validate(request).throwIfInvalid()
        repo.save(Invoice(id = request.id, amount = request.amount))
    }
}
