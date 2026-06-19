package sample.invoice

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import sample.invoice.commands.approveinvoice.ApproveInvoiceHandler
import sample.invoice.commands.createinvoice.CreateInvoiceHandler
import sample.invoice.commands.createinvoice.CreateInvoicePersistenceValidator
import sample.invoice.commands.createinvoice.CreateInvoiceRequestValidator
import sample.invoice.queries.getinvoice.GetInvoiceHandler
import sample.invoice.queries.streaminvoices.StreamInvoicesHandler

class InvoiceRegistrar(private val repo: InvoiceRepository) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateInvoiceHandler(repo)
            +ApproveInvoiceHandler(repo)
            +GetInvoiceHandler(repo)
            registerStream(StreamInvoicesHandler(repo))

            +CreateInvoiceRequestValidator()
            +CreateInvoicePersistenceValidator(repo)
        }
    }
}
