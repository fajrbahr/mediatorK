package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules
import sample.invoice.InvoiceRepository

sealed class CreateInvoiceError(val message: String) {
    data object IdBlank : CreateInvoiceError("Invoice ID is required")
    data object IdInvalidPrefix : CreateInvoiceError("Invoice ID must start with INV-")
    data object AmountNotPositive : CreateInvoiceError("Amount must be positive")
}

class CreateInvoiceRequestValidator : RequestValidator<CreateInvoiceCommand> {
    override fun validate(request: CreateInvoiceCommand) = rules<CreateInvoiceError> {
        check(request.id.isNotBlank()) { CreateInvoiceError.IdBlank }
        check(request.id.startsWith("INV-")) { CreateInvoiceError.IdInvalidPrefix }
        check(request.amount > 0) { CreateInvoiceError.AmountNotPositive }
    }
}

class CreateInvoicePersistenceValidator(private val repo: InvoiceRepository) : RequestValidator<CreateInvoiceCommand> {
    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (repo.findById(request.id) != null)
            ValidationResult.Invalid("Invoice ${request.id} already exists")
        else ValidationResult.Valid
}

class CreateInvoiceAmountPolicyValidator : RequestValidator<CreateInvoiceCommand> {
    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (request.amount <= 10_000.0) ValidationResult.Valid
        else ValidationResult.Invalid("Amount exceeds the maximum policy limit of 10,000")
}
