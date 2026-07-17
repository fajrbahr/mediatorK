package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.validator.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

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

// ── ValidationBehavior (via validate<T> DSL) ─────────────────────────────────

class ValidationBehaviorTest {

    @Test
    fun `valid request passes through to handler`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Valid }
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `invalid request throws ValidationException`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Invalid("validation failed") }
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `ValidationException carries the failure message`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Invalid("bad input") }
        }
        val ex = assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertNotNull(ex.message)
        assertTrue(ex.message!!.contains("bad input"))
    }

    @Test
    fun `no validator registered for request type - request passes through`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<AddCommand> { ValidationResult.Invalid("add bad") }
        }
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `ValidationBehavior runs before handler - handler not called on invalid request`() = runTest {
        var handlerCalled = false
        val m = mediatorK {
            handle<PingQuery, String> {
                handlerCalled = true
                "ok"
            }
            validate<PingQuery> { ValidationResult.Invalid("validation failed") }
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertFalse(handlerCalled)
    }

    @Test
    fun `ValidationBehavior with custom order participates in pipeline ordering`() = runTest {
        val log = mutableListOf<String>()
        val loggingB = behavior(order = -100) { _, _, next ->
            log += "outer"
            next()
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Valid }
            behaviors(loggingB)
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("outer"), log)
    }

    @Test
    fun `multiple validators for different types - only matching one runs`() = runTest {
        var addValidatorCalled = false
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Valid }
            validate<AddCommand> {
                addValidatorCalled = true
                ValidationResult.Valid
            }
        }
        m.send(PingQuery("x"))
        assertFalse(addValidatorCalled)
    }

    @Test
    fun `two validators for same type - first failure stops execution`() = runTest {
        var secondCalled = false
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Invalid("first fails") }
            validate<PingQuery> {
                secondCalled = true
                ValidationResult.Valid
            }
        }
        assertFailsWith<ValidationException> { m.send(PingQuery("x")) }
        assertFalse(secondCalled)
    }

    @Test
    fun `two validators for same type - both pass - handler is called`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Valid }
            validate<PingQuery> { ValidationResult.Valid }
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `validator returning Invalid raises ValidationException`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            validate<PingQuery> { ValidationResult.Invalid("direct") }
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
