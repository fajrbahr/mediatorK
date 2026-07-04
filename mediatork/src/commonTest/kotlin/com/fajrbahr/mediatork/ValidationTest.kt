package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.validator.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// ── rules DSL ─────────────────────────────────────────────────────────────────

class RulesDslTest {

    @Test
    fun `rules with all checks passing returns Valid`() {
        val result = rules {
            check(true) { "should not appear" }
            require(2 + 2 == 4) { "math broken" }
        }
        assertIs<ValidationResult.Valid>(result)
    }

    @Test
    fun `rules collects all failing checks`() {
        val result = rules {
            check(false) { "error 1" }
            check(false) { "error 2" }
            check(true) { "should not appear" }
        }
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(2, result.errors.size)
        assertTrue("error 1" in result.errors)
        assertTrue("error 2" in result.errors)
    }

    private enum class TestError { ONE, TWO }

    @Test
    fun `rules works with non-String error type`() {
        val result = rules {
            check(false) { TestError.ONE }
            check(false) { TestError.TWO }
        }
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(2, result.errors.size)
        assertTrue(TestError.ONE in result.errors)
        assertTrue(TestError.TWO in result.errors)
    }

    @Test
    fun `rules evaluates every rule even after first failure`() {
        val evaluated = mutableListOf<Int>()
        val result = rules {
            check(false) { evaluated += 1; "e1" }
            check(false) { evaluated += 2; "e2" }
            check(false) { evaluated += 3; "e3" }
        }
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(listOf(1, 2, 3), evaluated)
    }

    @Test
    fun `rules with no failures returns Valid`() {
        assertIs<ValidationResult.Valid>(rules<String> {})
    }
}

// ── rulesFailFast DSL ─────────────────────────────────────────────────────────

class RulesFailFastDslTest {

    @Test
    fun `rulesFailFast with all checks passing returns Valid`() {
        val result = rulesFailFast {
            check(true) { "no" }
            require(2 + 2 == 4) { "no" }
        }
        assertIs<ValidationResult.Valid>(result)
    }

    @Test
    fun `rulesFailFast returns first failing error only`() {
        val result = rulesFailFast {
            check(false) { "error 1" }
            check(false) { "error 2" }
        }
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(1, result.errors.size)
        assertTrue("error 1" in result.errors)
    }

    @Test
    fun `rulesFailFast skips remaining checks after first failure`() {
        val evaluated = mutableListOf<Int>()
        val result = rulesFailFast {
            check(true) { evaluated += 1; "no" }
            check(false) { evaluated += 2; "fail" }
            check(false) { evaluated += 3; "skipped" }
        }
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(listOf(2), evaluated)
    }

    @Test
    fun `rulesFailFast works with non-String error type`() {
        val result = rulesFailFast {
            check(false) { TestError.ONE }
            check(false) { TestError.TWO }
        }
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(1, result.errors.size)
        assertTrue(TestError.ONE in result.errors)
    }

    private enum class TestError { ONE, TWO }
}

// ── ValidationBehavior ────────────────────────────────────────────────────────

class ValidationBehaviorTest {

    private fun validatorFor(valid: Boolean, message: String = "validation failed"): RequestValidator<PingQuery> =
        object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult =
                if (valid) ValidationResult.Valid else ValidationResult.Invalid(message)
        }

    private fun behaviorWith(vararg validators: RequestValidator<PingQuery>) =
        validationBehavior(mapOf(PingQuery::class to validators.toList()))

    @Test
    fun `valid request passes through to handler`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(behaviorWith(validatorFor(valid = true)))) {
            register(PingHandler())
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `invalid request throws ValidationException`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(behaviorWith(validatorFor(valid = false)))) {
            register(PingHandler())
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `ValidationException carries the failure message`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(behaviorWith(validatorFor(valid = false, message = "bad input")))) {
            register(PingHandler())
        }
        val ex = assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertNotNull(ex.message)
        assertTrue(ex.message!!.contains("bad input"))
    }

    @Test
    fun `no validator registered for request type - request passes through`() = runTest {
        val addValidator = object : RequestValidator<AddCommand> {
            override fun validate(request: AddCommand): ValidationResult = ValidationResult.Invalid("add bad")
        }
        val m =
            mediator(pipelineBehaviors = listOf(validationBehavior(mapOf(AddCommand::class to listOf(addValidator))))) {
                register(PingHandler())
            }
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `ValidationBehavior runs before handler - handler not called on invalid request`() = runTest {
        var handlerCalled = false
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery,
            ): String {
                handlerCalled = true
                return "ok"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(behaviorWith(validatorFor(valid = false)))) {
            register(handler)
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertFalse(handlerCalled)
    }

    @Test
    fun `ValidationBehavior with custom order participates in pipeline ordering`() = runTest {
        val log = mutableListOf<String>()
        val loggingBehavior = object : PipelineBehavior {
            override val order = -100
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "outer"
                return next(request)
            }
        }
        val m = mediator(
            pipelineBehaviors = listOf(behaviorWith(validatorFor(valid = true)), loggingBehavior),
        ) {
            register(PingHandler())
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("outer"), log)
    }

    @Test
    fun `multiple validators for different types - only matching one runs`() = runTest {
        var addValidatorCalled = false
        val addValidator = object : RequestValidator<AddCommand> {
            override fun validate(request: AddCommand): ValidationResult {
                addValidatorCalled = true
                return ValidationResult.Valid
            }
        }
        val m = mediator(
            pipelineBehaviors = listOf(
                validationBehavior(
                    mapOf(
                        PingQuery::class to listOf(validatorFor(valid = true)),
                        AddCommand::class to listOf(addValidator),
                    )
                )
            ),
        ) {
            register(PingHandler())
        }
        m.send(PingQuery("x"))
        assertFalse(addValidatorCalled)
    }

    @Test
    fun `two validators for same type - first failure stops execution`() = runTest {
        var secondCalled = false
        val v1 = object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult = ValidationResult.Invalid("first fails")
        }
        val v2 = object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult {
                secondCalled = true
                return ValidationResult.Valid
            }
        }
        val m = mediator(pipelineBehaviors = listOf(behaviorWith(v1, v2))) {
            register(PingHandler())
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertFalse(secondCalled)
    }

    @Test
    fun `two validators for same type - both pass - handler is called`() = runTest {
        val m = mediator(
            pipelineBehaviors = listOf(
                behaviorWith(
                    validatorFor(valid = true), validatorFor(valid = true)
                )
            )
        ) {
            register(PingHandler())
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `validator returning Invalid raises ValidationException`() = runTest {
        val validator = object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult = ValidationResult.Invalid("direct")
        }
        val m = mediator(pipelineBehaviors = listOf(behaviorWith(validator))) {
            register(PingHandler())
        }
        val ex = assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertEquals("direct", ex.message)
    }
}

// ── AggregateException ────────────────────────────────────────────────────────

class AggregateExceptionTest {

    @Test
    fun `AggregateException message reports count`() {
        val ex = AggregateException(
            listOf(RuntimeException("a"), RuntimeException("b"), RuntimeException("c"))
        )
        assertTrue(ex.message!!.contains("3"))
    }

    @Test
    fun `AggregateException is a MediatorException`() {
        val ex: MediatorException = AggregateException(listOf(RuntimeException("x")))
        assertNotNull(ex)
    }

    @Test
    fun `AggregateException message includes individual error messages`() {
        val ex = AggregateException(listOf(RuntimeException("specific-error")))
        assertTrue(ex.message!!.contains("specific-error"))
    }

    @Test
    fun `AggregateException message falls back to class name when exception has no message`() {
        val ex = AggregateException(listOf(object : RuntimeException() {}))
        assertNotNull(ex.message)
    }

    @Test
    fun `AggregateException with mixed null and non-null messages includes all`() {
        val withMessage = RuntimeException("has-message")
        val withoutMessage = RuntimeException()
        val ex = AggregateException(listOf(withMessage, withoutMessage))
        assertTrue(ex.message!!.contains("has-message"))
        assertTrue(ex.message!!.contains("2"))
    }
}
