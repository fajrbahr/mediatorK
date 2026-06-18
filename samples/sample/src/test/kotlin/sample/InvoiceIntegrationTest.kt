package sample

import com.fajrbahr.mediatork.pipeline.TransactionPipelineBehavior
import com.fajrbahr.mediatork.test.buildHandlerTestHarness
import com.fajrbahr.mediatork.validator.ValidationBehavior
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import sample.invoice.Invoice
import sample.invoice.InvoiceRepository
import sample.invoice.InvoiceStatus
import sample.invoice.commands.approveinvoice.ApproveInvoiceCommand
import sample.invoice.commands.approveinvoice.ApproveInvoiceHandler
import sample.invoice.commands.createinvoice.CreateInvoiceCommand
import sample.invoice.commands.createinvoice.CreateInvoiceDomainValidator
import sample.invoice.commands.createinvoice.CreateInvoiceField
import sample.invoice.commands.createinvoice.CreateInvoiceHandler
import sample.invoice.commands.createinvoice.CreateInvoicePersistenceValidator
import sample.invoice.commands.createinvoice.CreateInvoiceRequestValidator
import sample.invoice.queries.getinvoice.GetInvoiceHandler
import sample.invoice.queries.getinvoice.GetInvoiceQuery
import sample.invoice.queries.streaminvoices.StreamInvoicesHandler
import sample.invoice.queries.streaminvoices.StreamInvoicesQuery
import kotlin.test.*

/**
 * Integration tests for the Invoice slice using [buildHandlerTestHarness].
 *
 * Each test wires a real mediator with real handlers — no mocks, no fakes.
 * Setup goes through the front door via [given], the action under test goes
 * through [send], and assertions read back via [query].
 */
class InvoiceIntegrationTest {

    private fun harness(repo: InvoiceRepository = InvoiceRepository()) =
        buildHandlerTestHarness(
            pipelineBehaviors = listOf(
                ValidationBehavior(listOf(CreateInvoiceRequestValidator())),
                TransactionPipelineBehavior(transactionProvider = repo.transactionProvider),
            ),
        ) {
            +CreateInvoiceHandler(
                repo = repo,
                domainValidator = CreateInvoiceDomainValidator(repo),
                persistenceValidator = CreateInvoicePersistenceValidator(repo),
            )
            +ApproveInvoiceHandler(repo)
            +GetInvoiceHandler(repo)
            registerStream(StreamInvoicesHandler(repo))
        }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `created invoice starts as PENDING`() = runTest {
        val h = harness()
        h.send(CreateInvoiceCommand(id = "INV-100", amount = 250.0))
        val invoice = h.query(GetInvoiceQuery(id = "INV-100"))
        assertEquals(InvoiceStatus.PENDING, invoice.status)
        assertEquals(250.0, invoice.amount)
    }

    @Test
    fun `approved invoice transitions to APPROVED`() = runTest {
        val h = harness()
        // Setup through the front door — same command path as production
        h.given(CreateInvoiceCommand(id = "INV-200", amount = 500.0))

        h.send(ApproveInvoiceCommand(id = "INV-200"))

        val invoice = h.query(GetInvoiceQuery(id = "INV-200"))
        assertEquals(InvoiceStatus.APPROVED, invoice.status)
    }

    @Test
    fun `setup via given creates multiple invoices`() = runTest {
        val repo = InvoiceRepository()
        val h = harness(repo)
        h.given(
            CreateInvoiceCommand(id = "INV-301", amount = 100.0),
            CreateInvoiceCommand(id = "INV-302", amount = 200.0),
            CreateInvoiceCommand(id = "INV-303", amount = 300.0),
        )
        assertEquals(3, repo.all().size)
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    fun `REQUEST scope rejects invalid ID format before handler runs`() = runTest {
        val h = harness()
        val ex = assertFailsWith<ValidationException> {
            h.send(CreateInvoiceCommand(id = "BADINPUT", amount = 100.0))
        }
        assertTrue(ex.errors.any { it.field is CreateInvoiceField.Id })
    }

    @Test
    fun `DOMAIN scope rejects duplicate invoice ID`() = runTest {
        val h = harness()
        h.given(CreateInvoiceCommand(id = "INV-400", amount = 100.0))

        val ex = assertFailsWith<ValidationException> {
            h.send(CreateInvoiceCommand(id = "INV-400", amount = 200.0))
        }
        assertTrue(ex.errors.any { it.field is CreateInvoiceField.Id })
    }

    // ── Transaction rollback ──────────────────────────────────────────────────

    @Test
    fun `transaction rolls back when approve fails — invoice stays PENDING`() = runTest {
        val h = harness()
        h.given(CreateInvoiceCommand(id = "INV-500", amount = 150.0))

        // Approving a non-existent ID throws inside the transaction
        assertFailsWith<IllegalStateException> {
            h.send(ApproveInvoiceCommand(id = "INV-MISSING"))
        }

        // INV-500 is unaffected — rollback preserved its state
        val invoice = h.query(GetInvoiceQuery(id = "INV-500"))
        assertEquals(InvoiceStatus.PENDING, invoice.status)
    }

    // ── Streaming ─────────────────────────────────────────────────────────────

    @Test
    fun `stream emits all invoices`() = runTest {
        val h = harness()
        h.given(
            CreateInvoiceCommand(id = "INV-S01", amount = 100.0),
            CreateInvoiceCommand(id = "INV-S02", amount = 200.0),
        )
        val all = h.stream(StreamInvoicesQuery()).toList()
        assertEquals(2, all.size)
    }

    @Test
    fun `stream filters by status`() = runTest {
        val h = harness()
        h.given(
            CreateInvoiceCommand(id = "INV-S10", amount = 100.0),
            CreateInvoiceCommand(id = "INV-S11", amount = 200.0),
        )
        h.send(ApproveInvoiceCommand(id = "INV-S10"))

        val approved = h.stream(StreamInvoicesQuery(status = InvoiceStatus.APPROVED)).toList()
        assertEquals(1, approved.size)
        assertEquals("INV-S10", approved.first().id)
    }

    @Test
    fun `cannot approve an already-approved invoice`() = runTest {
        val h = harness()
        h.given(CreateInvoiceCommand(id = "INV-600", amount = 400.0))
        h.send(ApproveInvoiceCommand(id = "INV-600"))

        assertFailsWith<IllegalStateException> {
            h.send(ApproveInvoiceCommand(id = "INV-600"))
        }
    }
}
