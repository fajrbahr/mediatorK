package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import sample.invoice.InvoiceRepository
import kotlin.reflect.KClass


class CreateInvoiceDomainValidator(private val repo: InvoiceRepository) : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class

    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (repo.findById(request.id) != null)
            ValidationResult.Invalid("Invoice ${request.id} already exists")
        else ValidationResult.Valid
}

class CreateInvoicePersistenceValidator(private val repo: InvoiceRepository) : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class

    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (repo.findById(request.id) != null)
            ValidationResult.Invalid("Duplicate invoice ID — database constraint violated")
        else ValidationResult.Valid
}

class CreateInvoiceAmountPolicyValidator : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class

    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (request.amount <= 10_000.0) ValidationResult.Valid
        else ValidationResult.Invalid("Amount exceeds the maximum policy limit of 10,000")
}
