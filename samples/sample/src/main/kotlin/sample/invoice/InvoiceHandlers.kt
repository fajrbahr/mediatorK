package sample.invoice

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.handler.StreamRequestHandler
import com.fajrbahr.mediatork.validator.ValidationException
import com.fajrbahr.mediatork.validator.ValidationScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter

class CreateInvoiceHandler(
    private val repo: InvoiceRepository,
    private val domainValidator: CreateInvoiceDomainValidator,
    private val persistenceValidator: CreateInvoicePersistenceValidator,
) : RequestHandler<CreateInvoiceCommand, Unit> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateInvoiceCommand) {
        // DOMAIN validation — runs here because it needs app state (the repository)
        val domainResult = domainValidator.validate(request)
        if (!domainResult.isValid) throw ValidationException(domainResult.errors)

        val invoice = Invoice(id = request.id, amount = request.amount)

        // PERSISTENCE validation — runs just before the write, inside the transaction
        val persistenceResult = persistenceValidator.validate(request)
        if (!persistenceResult.isValid) throw ValidationException(persistenceResult.errors)

        repo.save(invoice)
    }
}

class ApproveInvoiceHandler(
    private val repo: InvoiceRepository,
) : RequestHandler<ApproveInvoiceCommand, Unit> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: ApproveInvoiceCommand) {
        val invoice = repo.findById(request.id)
            ?: error("Invoice ${request.id} not found")

        check(invoice.status == InvoiceStatus.PENDING) {
            "Invoice ${request.id} cannot be approved — current status: ${invoice.status}"
        }

        repo.save(invoice.copy(status = InvoiceStatus.APPROVED))
    }
}

class GetInvoiceHandler(
    private val repo: InvoiceRepository,
) : RequestHandler<GetInvoiceQuery, Invoice> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: GetInvoiceQuery): Invoice =
        repo.findById(request.id) ?: error("Invoice ${request.id} not found")
}

class StreamInvoicesHandler(
    private val repo: InvoiceRepository,
) : StreamRequestHandler<StreamInvoicesQuery, Invoice> {
    override fun handle(mediator: Mediator, requestContext: RequestContext, request: StreamInvoicesQuery): Flow<Invoice> =
        repo.all().asFlow().let { flow ->
            if (request.status != null) flow.filter { it.status == request.status } else flow
        }
}

class InvoiceRegistrar(private val repo: InvoiceRepository) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateInvoiceHandler(
                repo = repo,
                domainValidator = CreateInvoiceDomainValidator(repo),
                persistenceValidator = CreateInvoicePersistenceValidator(repo),
            )
            +ApproveInvoiceHandler(repo)
            +GetInvoiceHandler(repo)
            registerStream(StreamInvoicesHandler(repo))
        }
    }
}
