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
- The handler should implement RequestHandler<TRequest, TResponse>
- Use constructor injection for dependencies
- Use suspend functions
```

---

## Create a Command (no return value)

```
Using MediatorK, create a command and its handler for:

[describe the action, e.g. "delete a user account by ID"]

Requirements:
- Request implements Request.Unit
- Handler implements RequestHandler<TRequest, Unit>
- Inject dependencies via constructor
```

---

## Create a Notification & Handler

```
Using MediatorK, create a notification and one or more handlers for:

[describe the event, e.g. "user signed up — send a welcome email and log the event"]

Requirements:
- Notification implements Notification
- Each handler implements NotificationHandler<TNotification>
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

## Create a Validation Behavior

```
Using MediatorK, create a validation pipeline behavior that validates requests before they reach their handler.

[describe validation rules, e.g. "ensure CreateOrderCommand has a non-blank cartId and a positive amount"]

Requirements:
- Use a ValidationBehavior or a generic PipelineBehavior
- Throw a descriptive exception on invalid input
- Short-circuit the pipeline — do not call next() if validation fails
```
