package sample.invoice

enum class InvoiceStatus { PENDING, APPROVED, REJECTED }

data class Invoice(
    val id: String,
    val amount: Double,
    val status: InvoiceStatus = InvoiceStatus.PENDING,
)
