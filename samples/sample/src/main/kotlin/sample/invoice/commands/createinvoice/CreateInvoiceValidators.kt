package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.validator.*
import sample.invoice.InvoiceRepository
import kotlin.reflect.KClass

class CreateInvoiceRequestValidator : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class
    override val scope = ValidationScope.REQUEST

    override fun validate(request: CreateInvoiceCommand): ValidationResult = rules {
        ruleFor(CreateInvoiceField.Id, request.id) {
            check(it.isNotBlank()) { "Invoice ID is required" }
            check(it.startsWith("INV-")) { "Invoice ID must start with INV-" }
        }
        ruleFor(CreateInvoiceField.Amount, request.amount) {
            check(it > 0) { "Amount must be positive" }
        }
    }
}

class CreateInvoiceDomainValidator(private val repo: InvoiceRepository) : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class
    override val scope = ValidationScope.DOMAIN

    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (repo.findById(request.id) != null) {
            ValidationResult.error(CreateInvoiceField.Id, "Invoice ${request.id} already exists")
        } else {
            ValidationResult.Success
        }
}

class CreateInvoicePersistenceValidator(private val repo: InvoiceRepository) : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class
    override val scope = ValidationScope.PERSISTENCE

    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (repo.findById(request.id) != null) {
            ValidationResult.error(CreateInvoiceField.Id, "Duplicate invoice ID — database constraint violated")
        } else {
            ValidationResult.Success
        }
}

class CreateInvoiceAmountPolicyValidator : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class
    override val scope = ValidationScope.REQUEST

    override fun validate(request: CreateInvoiceCommand): ValidationResult = rules {
        ruleFor(CreateInvoiceField.Amount, request.amount) {
            check(it <= 10_000.0) { "Amount exceeds the maximum policy limit of 10,000" }
        }
    }
}

sealed class CreateInvoiceField : FieldValidator {
    object Id : CreateInvoiceField()
    object Amount : CreateInvoiceField()
}
