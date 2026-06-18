package sample.invoice

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.pipeline.TransactionPipelineBehavior
import com.fajrbahr.mediatork.validator.ValidationBehavior
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import sample.invoice.commands.approveinvoice.ApproveInvoiceCommand
import sample.invoice.commands.createinvoice.CreateInvoiceAmountPolicyValidator
import sample.invoice.commands.createinvoice.CreateInvoiceCommand
import sample.invoice.commands.createinvoice.CreateInvoiceDomainValidator
import sample.invoice.commands.createinvoice.CreateInvoicePersistenceValidator
import sample.invoice.commands.createinvoice.CreateInvoiceRequestValidator
import sample.invoice.queries.getinvoice.GetInvoiceQuery
import sample.invoice.queries.streaminvoices.StreamInvoicesQuery

// ── Test 25: Transaction — commit on success ──────────────────────────────────

class Test25TransactionCommit {
    suspend fun start() {
        println("=== TEST 25: TransactionPipelineBehavior — changes committed on success ===")
        val repo = InvoiceRepository()
        val mediator = MediatorFactory.create(
            registrars = listOf(InvoiceRegistrar(repo)),
            pipelineBehaviors = listOf(
                ValidationBehavior(listOf(CreateInvoiceRequestValidator())),
                TransactionPipelineBehavior(transactionProvider = repo.transactionProvider),
            ),
        )

        mediator.send(CreateInvoiceCommand(id = "INV-001", amount = 500.0))
        mediator.send(ApproveInvoiceCommand(id = "INV-001"))

        val invoice = mediator.send(GetInvoiceQuery(id = "INV-001"))
        println("  Invoice ${invoice.id}: status=${invoice.status}, amount=${invoice.amount}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test25TransactionCommit().start() }
    }
}

// ── Test 26: Transaction — rollback on failure ────────────────────────────────

class Test26TransactionRollback {
    suspend fun start() {
        println("=== TEST 26: TransactionPipelineBehavior — partial writes rolled back on failure ===")
        val repo = InvoiceRepository()
        val mediator = MediatorFactory.create(
            registrars = listOf(InvoiceRegistrar(repo)),
            pipelineBehaviors = listOf(
                ValidationBehavior(listOf(CreateInvoiceRequestValidator())),
                TransactionPipelineBehavior(transactionProvider = repo.transactionProvider),
            ),
        )

        // Create two invoices so they exist in the store
        mediator.send(CreateInvoiceCommand(id = "INV-010", amount = 100.0))
        mediator.send(CreateInvoiceCommand(id = "INV-011", amount = 200.0))
        println("  Before: ${repo.all().map { "${it.id}=${it.status}" }}")

        // Approving a non-existent invoice throws inside the transaction — both writes roll back
        runCatching {
            mediator.send(ApproveInvoiceCommand(id = "INV-MISSING"))
        }.onFailure { println("  Caught expected error: ${it.message}") }

        // INV-010 and INV-011 are untouched because the transaction was rolled back
        println("  After rollback: ${repo.all().map { "${it.id}=${it.status}" }}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test26TransactionRollback().start() }
    }
}

// ── Test 27: ValidationScope — REQUEST runs in pipeline, DOMAIN/PERSISTENCE in handler ──

// ── Test 28: StreamRequest — lazy flow, no batching ───────────────────────────

class Test28StreamInvoices {
    suspend fun start() {
        println("=== TEST 28: StreamRequest<Invoice> — stream all approved invoices ===")
        val repo = InvoiceRepository()
        val mediator = MediatorFactory.create(
            registrars = listOf(InvoiceRegistrar(repo)),
            pipelineBehaviors = listOf(
                ValidationBehavior(listOf(CreateInvoiceRequestValidator())),
            ),
        )

        // Seed data
        listOf("INV-A1" to 100.0, "INV-A2" to 200.0, "INV-A3" to 300.0).forEach { (id, amt) ->
            mediator.send(CreateInvoiceCommand(id, amt))
        }
        mediator.send(ApproveInvoiceCommand("INV-A1"))
        mediator.send(ApproveInvoiceCommand("INV-A3"))

        // Stream — collect approved invoices one by one, no List allocated up front
        println("  Streaming approved invoices:")
        mediator.stream(StreamInvoicesQuery(status = InvoiceStatus.APPROVED))
            .collect { invoice -> println("    ${invoice.id} — \$${invoice.amount}") }

        // Stream all — no filter
        val all = mediator.stream(StreamInvoicesQuery()).toList()
        println("  Total invoices in store: ${all.size}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test28StreamInvoices().start() }
    }
}

// ── Test 29: Multiple validators — both run, errors merged ───────────────────

class Test29MultipleValidators {
    suspend fun start() {
        println("=== TEST 29: Multiple validators for the same command — all run, errors merged ===")
        val repo = InvoiceRepository()
        val mediator = MediatorFactory.create(
            registrars = listOf(InvoiceRegistrar(repo)),
            pipelineBehaviors = listOf(
                // Two REQUEST-scope validators registered for CreateInvoiceCommand.
                // ValidationBehavior runs ALL matching validators and merges their errors.
                ValidationBehavior(
                    listOf(
                        CreateInvoiceRequestValidator(),      // checks id format + amount > 0
                        CreateInvoiceAmountPolicyValidator(), // checks amount <= 10,000
                    )
                ),
            ),
        )

        // Both validators fail: bad ID format (v1) + amount over limit (v2) → two errors
        println("  [BOTH FAIL] bad id + amount over limit:")
        runCatching {
            mediator.send(CreateInvoiceCommand(id = "BADINPUT", amount = 15_000.0))
        }.onFailure { ex ->
            if (ex is ValidationException) ex.errors.forEach { println("    field=${it.field}, msg=${it.message}") }
        }

        // Only the policy validator fails: valid id, amount over limit → one error
        println("  [POLICY ONLY] valid id, amount over limit:")
        runCatching {
            mediator.send(CreateInvoiceCommand(id = "INV-099", amount = 20_000.0))
        }.onFailure { ex ->
            if (ex is ValidationException) ex.errors.forEach { println("    field=${it.field}, msg=${it.message}") }
        }

        // Both pass → handler runs
        println("  [BOTH PASS] valid id + amount within limit:")
        mediator.send(CreateInvoiceCommand(id = "INV-100", amount = 500.0))
        println("    Invoice created: ${repo.findById("INV-100")?.id}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test29MultipleValidators().start() }
    }
}

class Test27ValidationScopes {
    suspend fun start() {
        println("=== TEST 27: ValidationScope — three scopes, each at the right layer ===")
        val repo = InvoiceRepository()
        val mediator = MediatorFactory.create(
            registrars = listOf(InvoiceRegistrar(repo)),
            pipelineBehaviors = listOf(
                // Only CreateInvoiceRequestValidator runs here — scope == REQUEST
                ValidationBehavior(
                    listOf(
                        CreateInvoiceRequestValidator(),         // REQUEST  → runs in pipeline
                        CreateInvoiceDomainValidator(repo),      // DOMAIN   → skipped by pipeline
                        CreateInvoicePersistenceValidator(repo), // PERSISTENCE → skipped by pipeline
                    )
                ),
            ),
        )

        // REQUEST scope catches this before the handler even starts
        println("  [REQUEST] missing INV- prefix:")
        runCatching {
            mediator.send(CreateInvoiceCommand(id = "BADINPUT", amount = 100.0))
        }.onFailure { ex ->
            if (ex is ValidationException) ex.errors.forEach { println("    field=${it.field}, msg=${it.message}") }
        }

        // Valid request reaches the handler; DOMAIN scope catches duplicate ID
        mediator.send(CreateInvoiceCommand(id = "INV-020", amount = 300.0))
        println("  [DOMAIN] duplicate ID:")
        runCatching {
            mediator.send(CreateInvoiceCommand(id = "INV-020", amount = 300.0))
        }.onFailure { ex ->
            if (ex is ValidationException) ex.errors.forEach { println("    field=${it.field}, msg=${it.message}") }
        }

        // PERSISTENCE scope would catch any constraint that slipped past domain checks
        println("  All scopes exercised correctly.")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test27ValidationScopes().start() }
    }
}
