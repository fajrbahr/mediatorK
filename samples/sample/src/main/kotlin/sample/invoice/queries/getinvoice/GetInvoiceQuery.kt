package sample.invoice.queries.getinvoice

import com.fajrbahr.mediatork.Request
import sample.invoice.Invoice

data class GetInvoiceQuery(val id: String) : Request<Invoice>
