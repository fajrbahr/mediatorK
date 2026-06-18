package com.fajrbahr.mediatork.validator

/**
 * Classifies the lifecycle stage at which a [RequestValidator] applies.
 *
 * Separating validators by scope keeps each layer honest about what information it has:
 *
 * - **[REQUEST]** — field-format and type checks answerable from the incoming data alone.
 *   Run automatically via [ValidationBehavior] before the handler executes.
 *   Example: "email must not be blank", "quantity must be > 0".
 *
 * - **[DOMAIN]** — business-rule checks that require application state.
 *   Call these explicitly inside command handlers or domain services after loading
 *   the relevant aggregate.
 *   Example: "account must be active", "order total must not exceed credit limit".
 *
 * - **[PERSISTENCE]** — constraints that can only be evaluated against the database.
 *   Call these inside a handler just before the write, optionally inside a transaction.
 *   Example: "email must be unique", "product SKU must not already exist".
 *
 * [ValidationBehavior] only runs [REQUEST]-scoped validators by default, so [DOMAIN]
 * and [PERSISTENCE] validators never execute before the handler has loaded the context
 * they need.
 */
enum class ValidationScope {
    REQUEST,
    DOMAIN,
    PERSISTENCE,
}
