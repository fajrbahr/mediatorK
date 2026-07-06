---
id: ai-prompts
title: AI Prompts
sidebar_label: AI Prompts
---

# AI Prompts

Ready-to-use prompts for generating MediatorK code with any AI assistant (Claude, Copilot, ChatGPT, etc.).

---

## Create a Request & Handler

```
Using MediatorK for Kotlin/Android, create a request and handler for the following use case:

[describe what it does, e.g. "fetch a list of products from the repository"]

Requirements:
- The request should be a data class implementing Request<T> (or Request.Unit if no return value)
- The handler should use the registry.handle<TRequest, TResponse> DSL block
- Inject dependencies into the registrar/module where the DSL block is defined
- Use suspend blocks within the DSL
```

---

## Create a Command (no return value)

```
Using MediatorK, create a command and its handler for:

[describe the action, e.g. "delete a user account by ID"]

Requirements:
- Request implements Request.Unit
- Handler uses the registry.handle<TRequest, Unit> DSL block
- Inject dependencies into the registrar/module where the DSL block is defined
```

---

## Create a Notification & Handler

```
Using MediatorK, create a notification and one or more handlers for:

[describe the event, e.g. "user signed up — send a welcome email and log the event"]

Requirements:
- Notification implements Notification
- Each handler uses the registry.on<TNotification> DSL block
- Handlers are independent — each handles a single concern
```

---

## Create a Pipeline Behavior

```
Using MediatorK, create a pipeline behavior that:

[describe the cross-cutting concern, e.g. "logs the request name and execution time for every request"]

Requirements:
- Implement PipelineBehavior
- Use the process() suspend function and call next()
- Keep it generic so it applies to all request types
```

---

## Create a ViewModel with Mediator

```
Using MediatorK with Android ViewModel, create a ViewModel for the following screen:

[describe the screen, e.g. "product list screen that loads products and supports pull-to-refresh"]

Requirements:
- Inject Mediator via constructor (works with Hilt, Koin, or manual DI)
- Send requests using mediator.send()
- Expose state via StateFlow or Flow
- Handle loading and error states
```

---

## Validation with DSL

```
Using MediatorK, add validation to a request using the registry.validate<T> DSL:

[describe validation rules, e.g. "ensure CreateOrderCommand has a non-blank cartId and a positive amount"]

Requirements:
- Use registry.validate<TRequest> { } DSL block
- Validation runs automatically before the handler via ValidationBehavior
- Inject dependencies into the validator block as needed
- Throw ValidationException or use result builders for error handling
```

---

## Error Handling & Exception Strategy

```
Using MediatorK, implement error handling and exception strategy for:

[describe the scenario, e.g. "handle different error types in a payment request — invalid amount, network failure, authorization error"]

Requirements:
- Define custom exceptions or use Result<T> patterns
- Handle exceptions in the handler using try-catch or Result builders
- Use PipelineException or domain-specific exceptions
- Log exceptions via pipeline behaviors if needed
- Map exceptions to user-friendly responses in ViewModels
```

---

## Chaining Requests & Composite Handlers

```
Using MediatorK, create a handler that chains multiple requests together:

[describe the composite operation, e.g. "when creating an order, first validate inventory, then create order, then send confirmation notification"]

Requirements:
- Inject Mediator into the handler
- Use mediator.send() to chain requests sequentially
- Handle errors from each request appropriately
- Compose notifications after main operations complete
```

---

## Custom Pipeline Behavior for Cross-Cutting Concerns

```
Using MediatorK, create a pipeline behavior for:

[describe the cross-cutting concern, e.g. "measure execution time and log slow requests (>500ms)"]

Requirements:
- Implement PipelineBehavior<TRequest, TResponse>
- Use process() suspend function and call next()
- Access request metadata (class name, etc.)
- Log or measure appropriately
- Ensure it's generic and reusable across all request types
```

---

## Dependency Injection & Module Registration

```
Using MediatorK with Hilt/Koin, set up the mediator and register handlers:

[describe the module, e.g. "an authentication module with login and logout handlers"]

Requirements:
- Create a registrar block with registry.handle<T, R> { } and registry.on<N> { } DSL blocks
- Register all handlers, validations, and behaviors in one place
- Inject the Mediator into ViewModels or services
- Support hot-swapping implementations for testing
```

---

## Notification Broadcast with Multiple Handlers

```
Using MediatorK, create a notification and independent handlers for different concerns:

[describe the event, e.g. "user account created — send welcome email, log audit event, update user stats"]

Requirements:
- Notification implements Notification
- Each handler uses registry.on<TNotification> { } independently
- Handlers run in sequence (or parallel if configured)
- Each handler focuses on one concern only
- Handlers don't depend on each other
```

---

## Testing Mediator Handlers

```
Using MediatorK in tests, create unit tests for handlers:

[describe what to test, e.g. "test that CreateUserCommand validates email and calls the repository"]

Requirements:
- Use a test Mediator instance or mock injected dependencies
- Test both happy path and error scenarios
- Verify that mediator.send() returns expected results
- Mock external dependencies (repositories, APIs, services)
- Assert side effects (notifications sent, state changed)
```

---

## Type-Safe Request & Response Patterns

```
Using MediatorK, create strongly-typed requests and responses for:

[describe the feature, e.g. "fetch user profile with nested data (user, orders, payments)"]

Requirements:
- Define Request<T> with specific response type T
- Use sealed classes or enums for related requests
- Leverage Kotlin's type system for compile-time safety
- Handle generic types in handlers if needed
- Ensure ViewModels receive correctly-typed responses
```
