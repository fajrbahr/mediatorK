package sample.invoice.commands.approveinvoice

import com.fajrbahr.mediatork.api.Request

data class ApproveInvoiceCommand(val id: String) : Request.Unit
