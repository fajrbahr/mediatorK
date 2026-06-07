# Spring Boot 3.x Examples

This example demonstrates how to use Kordinator on raw Kotlin project without any framework

## Prerequisites

Create empty Kotlin project

## Step 1: Add Kordinator dependency

### Maven

Add Kordinator dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.ceviz</groupId>
    <artifactId>kordinator</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (Groovy)

Add Kordinator dependency to your `build.gradle`:

```groovy
implementation 'dev.ceviz:kordinator:1.0.0'
```

### Gradle (Kotlin)

Add Kordinator dependency to your `build.gradle.kts`:

```kotlin
implementation("dev.ceviz:kordinator:1.0.0")
```

## Step 2: Create a simple app

When we create a basic app with Kotlin, we will see basic main function in generated code like below:

```kotlin
fun main() {
    println("Hello, World!")
}
```

Now we will add command and query to run our app with Kordinator:

```kotlin

data class TestCommand(
    val message: String
) : Command

data class TestQuery(
    val type: String
) : Query<String>

```

## Step 3: Implement Command and Query Handlers

```kotlin

class TestCommandHandler : CommandHandler<TestCommand> {
    override suspend fun handle(command: TestCommand) {
        println("Test command message: ${command.message}")
    }
}

class TestQueryHandler : QueryHandler<TestQuery, String> {
    override suspend fun handle(query: TestQuery): String {
        if (query.type == "info") {
            return "Info Test query type: ${query.type}"
        } else if (query.type == "error") {
            return "Error Test query type: ${query.type}"
        } else if (query.type == "warm") {
            return "Warm Test query type: ${query.type}"
        }

        return "Test query type: ${query.type}"
    }
}

```

## Step 4: Register Command and Query Handlers

Now we have to implement simple Dependency Injection to register our handlers:

```kotlin

class TestDependencyFactory(
    private val handlerMap: HashMap<Class<*>, Any>
) : DependencyFactory {
    override fun <T> getSubTypesOf(clazz: Class<T>): Collection<Class<T>> {
        return handlerMap
            .filter {
                it.key.interfaces.contains(clazz)
            }
            .map {
                it.key as Class<T>
            }
    }

    override fun <T> getInstanceOf(clazz: Class<T>): T {
        return handlerMap[clazz] as T
    }
}

```

## Step 5: Use Kordinator with these command and query

Let's start to implement and define in main function

```kotlin

suspend fun main() {
    // Initialize handlers
    val testCommandHandler = TestCommandHandler()
    val testQueryHandler = TestQueryHandler()
    
    // Initialize Dependency Factory
    val dependencyFactory = TestDependencyFactory(
        hashMapOf(
            TestCommandHandler::class.java to testCommandHandler,
            TestQueryHandler::class.java to testQueryHandler
        )
    )

    // Initialize Mediator
    val mediator = MediatorBuilder(dependencyFactory)
        .build()

    // Send query and command
    val message = mediator.send(TestQuery("info"))
    mediator.send(TestCommand(message))
}

```

## Step 6: Run your app

Now you can run your app and see the output:

```shell
./gradlew run
```
