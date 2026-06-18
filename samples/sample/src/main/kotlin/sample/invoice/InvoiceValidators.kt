package sample.invoice

import com.fajrbahr.mediatork.validator.*
import kotlin.reflect.KClass

// ── REQUEST scope — field format/type checks, runs automatically in the pipeline ──

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

// ── DOMAIN scope — business-rule checks requiring app state, called by the handler ──

class CreateInvoiceDomainValidator(private val repo: InvoiceRepository) : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class
    override val scope = ValidationScope.DOMAIN

    override fun validate(request: CreateInvoiceCommand): ValidationResult {
        // Business rule: the same invoice ID cannot be re-submitted
        return if (repo.findById(request.id) != null) {
            ValidationResult.error(CreateInvoiceField.Id, "Invoice ${request.id} already exists")
        } else {
            ValidationResult.Success
        }
    }
}

// ── PERSISTENCE scope — uniqueness / FK checks, called just before the write ──

class CreateInvoicePersistenceValidator(private val repo: InvoiceRepository) : RequestValidator<CreateInvoiceCommand> {
    override val requestClass: KClass<CreateInvoiceCommand> = CreateInvoiceCommand::class
    override val scope = ValidationScope.PERSISTENCE

    override fun validate(request: CreateInvoiceCommand): ValidationResult {
        // DB-level check: ID uniqueness (here same as domain, but in production this
        // would be a SELECT EXISTS query inside the same transaction as the insert)
        return if (repo.findById(request.id) != null) {
            ValidationResult.error(CreateInvoiceField.Id, "Duplicate invoice ID — database constraint violated")
        } else {
            ValidationResult.Success
        }
    }
}

sealed class CreateInvoiceField : FieldValidator {
    object Id : CreateInvoiceField()
    object Amount : CreateInvoiceField()
}
