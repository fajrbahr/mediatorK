package sample.invoice.queries.getinvoice

import com.fajrbahr.mediatork.api.Request
import sample.invoice.Invoice

data class GetInvoiceQuery(val id: String) : Request<Invoice>
