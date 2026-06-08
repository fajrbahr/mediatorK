package com.fajrbahr.mediatork

import kotlin.test.*

class RequestContextTest {

    @Test
    fun `put and get returns stored value`() {
        val ctx = RequestContext()
        ctx.put("key", "value")
        assertEquals("value", ctx.getMetaDate<String>("key"))
    }

    @Test
    fun `get returns null for absent key`() {
        val ctx = RequestContext()
        assertNull(ctx.getMetaDate<String>("missing"))
    }

    @Test
    fun `put overwrites existing value`() {
        val ctx = RequestContext()
        ctx.put("key", "first")
        ctx.put("key", "second")
        assertEquals("second", ctx.getMetaDate<String>("key"))
    }

    @Test
    fun `put stores null and get returns null`() {
        val ctx = RequestContext()
        ctx.put("key", null)
        assertNull(ctx.getMetaDate<String>("key"))
    }

    @Test
    fun `get with correct type returns value`() {
        val ctx = RequestContext()
        ctx.put("key", 42)
        assertEquals(42, ctx.getMetaDate<Int>("key"))
    }

    @Test
    fun `multiple keys are independent`() {
        val ctx = RequestContext()
        ctx.put("a", 1)
        ctx.put("b", 2)
        assertEquals(1, ctx.getMetaDate<Int>("a"))
        assertEquals(2, ctx.getMetaDate<Int>("b"))
    }

    @Test
    fun `stores arbitrary non-string value`() {
        data class Payload(val id: Int)
        val ctx = RequestContext()
        ctx.put("payload", Payload(7))
        assertEquals(Payload(7), ctx.getMetaDate<Payload>("payload"))
    }

    @Test
    fun `fresh RequestContext is empty`() {
        val ctx = RequestContext()
        assertNull(ctx.getMetaDate<Any>("anything"))
    }

    @Test
    fun `two contexts are independent`() {
        val ctx1 = RequestContext()
        val ctx2 = RequestContext()
        ctx1.put("x", "from-ctx1")
        assertNull(ctx2.getMetaDate<String>("x"))
    }
}
