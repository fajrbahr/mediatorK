package sample.invoice.commands.createinvoice

import com.fajrbahr.mediatork.api.Request

data class CreateInvoiceCommand(val id: String, val amount: Double) : Request.Unit
