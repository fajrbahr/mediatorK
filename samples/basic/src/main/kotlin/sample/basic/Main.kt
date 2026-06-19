package sample.basic

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.MediatorRegistrar
import kotlinx.coroutines.runBlocking

// ── Entry point ───────────────────────────────────────────────────────────────

fun main() = runBlocking {
    val store = TodoStore()
    val mediator = MediatorFactory.create(registrars = listOf(TodoRegistrar(store)))

    val todo = mediator.send(AddTodoCommand(id = "1", title = "Buy groceries"))
    println("Created: $todo")

    val found = mediator.send(GetTodoQuery(id = "1"))
    println("Found: $found")

    val missing = mediator.send(GetTodoQuery(id = "999"))
    println("Missing: $missing")
}
