package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import com.fajrbahr.mediatork.validator.*
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.*

// ── Test fields ───────────────────────────────────────────────────────────────

private enum class Field : FieldValidator { Name, Email, Age }

// ── ValidationResult ──────────────────────────────────────────────────────────

class ValidationResultTest {

    @Test
    fun `Success is valid with no errors`() {
        val r = ValidationResult.Success
        assertTrue(r.isValid)
        assertTrue(r.errors.isEmpty())
    }

    @Test
    fun `error(message) produces invalid result with one error`() {
        val r = ValidationResult.error("bad input")
        assertFalse(r.isValid)
        assertEquals(1, r.errors.size)
        assertEquals("bad input", r.errors[0].message)
        assertEquals(DefaultField, r.errors[0].field)
    }

    @Test
    fun `error(field, message) associates error with field`() {
        val r = ValidationResult.error(Field.Email, "invalid email")
        assertFalse(r.isValid)
        assertEquals(Field.Email, r.errors[0].field)
        assertEquals("invalid email", r.errors[0].message)
    }

    @Test
    fun `failure(vararg) packs all errors`() {
        val r = ValidationResult.failure(
            com.fajrbahr.mediatork.validator.ValidationError(Field.Name, "too short"),
            com.fajrbahr.mediatork.validator.ValidationError(Field.Email, "missing @"),
        )
        assertFalse(r.isValid)
        assertEquals(2, r.errors.size)
    }

    @Test
    fun `ValidationResult with empty list is valid`() {
        assertTrue(ValidationResult(emptyList()).isValid)
    }

    @Test
    fun `ValidationResult with non-empty list is invalid`() {
        val r = ValidationResult(listOf(com.fajrbahr.mediatork.validator.ValidationError(message = "x")))
        assertFalse(r.isValid)
    }
}

// ── rules DSL ─────────────────────────────────────────────────────────────────

class RulesDslTest {

    @Test
    fun `rules with all checks passing returns Success`() {
        val r = rules {
            check(true) { "should not appear" }
            check(2 + 2 == 4) { "math broken" }
        }
        assertTrue(r.isValid)
    }

    @Test
    fun `rules collects all failing checks`() {
        val r = rules {
            check(false) { "error 1" }
            check(false) { "error 2" }
            check(true) { "should not appear" }
        }
        assertFalse(r.isValid)
        assertEquals(2, r.errors.size)
        assertTrue(r.errors.any { it.message == "error 1" })
        assertTrue(r.errors.any { it.message == "error 2" })
    }

    @Test
    fun `rules does not stop at first failure`() {
        val evaluated = mutableListOf<Int>()
        rules {
            check(false) { evaluated += 1; "e1" }
            check(false) { evaluated += 2; "e2" }
            check(false) { evaluated += 3; "e3" }
        }
        assertEquals(listOf(1, 2, 3), evaluated)
    }

    @Test
    fun `rules ruleFor attaches errors to named field`() {
        val r = rules {
            ruleFor(Field.Name, "") { value ->
                check(value.isNotBlank()) { "must not be blank" }
            }
        }
        assertFalse(r.isValid)
        assertEquals(Field.Name, r.errors[0].field)
        assertEquals("must not be blank", r.errors[0].message)
    }

    @Test
    fun `rules ruleFor with passing value produces no error`() {
        val r = rules {
            ruleFor(Field.Name, "Alice") { value ->
                check(value.isNotBlank()) { "must not be blank" }
            }
        }
        assertTrue(r.isValid)
    }

    @Test
    fun `rules ruleFor collects multiple field errors`() {
        val r = rules {
            ruleFor(Field.Email, "bad") { value ->
                check(value.contains('@')) { "missing @" }
                check(value.length > 5) { "too short" }
            }
        }
        assertEquals(2, r.errors.size)
        assertTrue(r.errors.all { it.field == Field.Email })
    }

    @Test
    fun `rules empty block returns Success`() {
        assertTrue(rules {}.isValid)
    }
}

// ── rulesFailFast DSL ─────────────────────────────────────────────────────────

class RulesFailFastDslTest {

    @Test
    fun `rulesFailFast with all checks passing returns Success`() {
        val r = rulesFailFast {
            check(true) { "nope" }
            check(true) { "nope2" }
        }
        assertTrue(r.isValid)
    }

    @Test
    fun `rulesFailFast stops after first failure`() {
        val evaluated = mutableListOf<Int>()
        rulesFailFast {
            check(false) { evaluated += 1; "e1" }
            check(false) { evaluated += 2; "e2" }
        }
        assertEquals(listOf(1), evaluated)
    }

    @Test
    fun `rulesFailFast returns exactly one error on multiple failures`() {
        val r = rulesFailFast {
            check(false) { "first" }
            check(false) { "second" }
        }
        assertFalse(r.isValid)
        assertEquals(1, r.errors.size)
        assertEquals("first", r.errors[0].message)
    }

    @Test
    fun `rulesFailFast ruleFor stops at first field error`() {
        val evaluated = mutableListOf<Int>()
        val r = rulesFailFast {
            ruleFor(Field.Email, "bad") { value ->
                check(value.contains('@')) { evaluated += 1; "missing @" }
                check(value.length > 5) { evaluated += 2; "too short" }
            }
        }
        assertFalse(r.isValid)
        assertEquals(1, r.errors.size)
        assertEquals(listOf(1), evaluated)
    }

    @Test
    fun `rulesFailFast skips later top-level checks after ruleFor fails`() {
        val evaluated = mutableListOf<Int>()
        rulesFailFast {
            ruleFor(Field.Name, "") { value ->
                check(value.isNotBlank()) { evaluated += 1; "blank" }
            }
            check(false) { evaluated += 2; "should not run" }
        }
        assertEquals(listOf(1), evaluated)
    }

    @Test
    fun `rulesFailFast empty block returns Success`() {
        assertTrue(rulesFailFast {}.isValid)
    }
}

// ── ValidationBehavior ────────────────────────────────────────────────────────

class ValidationBehaviorTest {

    private fun validatorFor(
        result: ValidationResult,
    ): RequestValidator<PingQuery> = object : RequestValidator<PingQuery> {
        override val requestClass: KClass<PingQuery> = PingQuery::class
        override fun validate(request: PingQuery): ValidationResult = result
    }

    @Test
    fun `valid request passes through to handler`() = runTest {
        val validator = validatorFor(ValidationResult.Success)
        val m = mediator(pipelineBehaviors = listOf(ValidationBehavior(listOf(validator)))) {
            register(PingHandler())
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `invalid request throws ValidationException`() = runTest {
        val validator = validatorFor(ValidationResult.error("bad"))
        val m = mediator(pipelineBehaviors = listOf(ValidationBehavior(listOf(validator)))) {
            register(PingHandler())
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `ValidationException carries the validation errors`() = runTest {
        val validator = validatorFor(
            ValidationResult.failure(
                com.fajrbahr.mediatork.validator.ValidationError(Field.Name, "too short"),
                com.fajrbahr.mediatork.validator.ValidationError(Field.Email, "invalid"),
            )
        )
        val m = mediator(pipelineBehaviors = listOf(ValidationBehavior(listOf(validator)))) {
            register(PingHandler())
        }
        val ex = assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertEquals(2, ex.errors.size)
        assertTrue(ex.errors.any { it.field == Field.Name })
        assertTrue(ex.errors.any { it.field == Field.Email })
    }

    @Test
    fun `no validator registered for request type - request passes through`() = runTest {
        val addValidator = object : RequestValidator<AddCommand> {
            override val requestClass: KClass<AddCommand> = AddCommand::class
            override fun validate(request: AddCommand): ValidationResult = ValidationResult.error("add bad")
        }
        val m = mediator(pipelineBehaviors = listOf(ValidationBehavior(listOf(addValidator)))) {
            register(PingHandler())
        }
        // PingQuery has no validator - must not throw
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `ValidationBehavior runs before handler - handler not called on invalid request`() = runTest {
        var handlerCalled = false
        val validator = validatorFor(ValidationResult.error("invalid"))
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
        val m = mediator(pipelineBehaviors = listOf(ValidationBehavior(listOf(validator)))) {
            register(handler)
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertFalse(handlerCalled)
    }

    @Test
    fun `ValidationBehavior with custom order participates in pipeline ordering`() = runTest {
        val log = mutableListOf<String>()
        val validator = validatorFor(ValidationResult.Success)
        val loggingBehavior = object : PipelineBehavior {
            override val order = -100 // runs before ValidationBehavior default (-50)
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
            pipelineBehaviors = listOf(ValidationBehavior(listOf(validator)), loggingBehavior),
        ) {
            register(PingHandler())
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("outer"), log)
    }

    @Test
    fun `multiple validators - only matching one runs`() = runTest {
        var addValidatorCalled = false
        val pingValidator = validatorFor(ValidationResult.Success)
        val addValidator = object : RequestValidator<AddCommand> {
            override val requestClass: KClass<AddCommand> = AddCommand::class
            override fun validate(request: AddCommand): ValidationResult {
                addValidatorCalled = true
                return ValidationResult.Success
            }
        }
        val m = mediator(
            pipelineBehaviors = listOf(ValidationBehavior(listOf(pingValidator, addValidator))),
        ) {
            register(PingHandler())
        }
        m.send(PingQuery("x"))
        assertFalse(addValidatorCalled)
    }

    @Test
    fun `ValidationException message contains field and error info`() = runTest {
        val validator = validatorFor(ValidationResult.error(Field.Name, "too short"))
        val m = mediator(pipelineBehaviors = listOf(ValidationBehavior(listOf(validator)))) {
            register(PingHandler())
        }
        val ex = assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertNotNull(ex.message)
        assertTrue(ex.message!!.contains("too short"))
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
}
