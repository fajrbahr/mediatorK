package sample.invoice

data class Invoice(
    val id: String,
    val amount: Double,
    val status: InvoiceStatus = InvoiceStatus.PENDING,
)

enum class InvoiceStatus { PENDING, APPROVED }
