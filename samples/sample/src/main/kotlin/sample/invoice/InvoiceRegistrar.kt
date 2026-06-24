package sample.invoice

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import sample.invoice.commands.approveinvoice.ApproveInvoiceHandler
import sample.invoice.commands.createinvoice.CreateInvoiceDomainValidator
import sample.invoice.commands.createinvoice.CreateInvoiceHandler
import sample.invoice.commands.createinvoice.CreateInvoicePersistenceValidator
import sample.invoice.queries.getinvoice.GetInvoiceHandler
import sample.invoice.queries.streaminvoices.StreamInvoicesHandler

class InvoiceRegistrar(private val repo: InvoiceRepository = InvoiceRepository()) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register CreateInvoiceHandler(
            repo = repo,
            domainValidator = CreateInvoiceDomainValidator(repo),
            persistenceValidator = CreateInvoicePersistenceValidator(repo),
        )
        registry register ApproveInvoiceHandler(repo)
        registry register GetInvoiceHandler(repo)
        registry.registerStream(StreamInvoicesHandler(repo))
    }
}
