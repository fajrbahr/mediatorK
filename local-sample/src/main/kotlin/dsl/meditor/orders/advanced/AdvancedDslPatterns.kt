package dsl.meditor.orders.advanced

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.api.contextKey
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validate
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.handler.orElse
import com.fajrbahr.mediatork.mediatorModule

/**
 * Advanced DSL patterns demonstration:
 * - Infix operator: `register` syntax
 * - Operator invoke: `feature()` and `module()` shorthand
 * - RequestContext get/set operators
 */

// ── Request types ──────────────────────────────────────────────────────────

data class ApproveOrderCommand(val orderId: String, val amount: Double) : Request<ApprovalResult>

data class ApprovalResult(val orderId: String, val approved: Boolean, val approverName: String)

// ── Context key for storing user info ──────────────────────────────────────

val USER_CONTEXT_KEY = contextKey<String>("user")
val APPROVAL_LEVEL_KEY = contextKey<String>("approval_level")

// ── Validator ──────────────────────────────────────────────────────────────

val approvalValidator: RequestValidator<ApproveOrderCommand> = validate<ApproveOrderCommand> {
    check(request.orderId.isNotBlank(), "Order ID required")
    check(request.amount > 0, "Amount must be positive")
}

// ── Handlers with fallback (demonstrates `orElse` infix operator) ──────────

val approveWithHigherAuthority = handler<ApproveOrderCommand, ApprovalResult> { request ->
    val user = context[USER_CONTEXT_KEY] ?: "unknown"
    println("  [MANAGER] $user approved high-value order ${request.orderId}")
    ApprovalResult(
        orderId = request.orderId,
        approved = true,
        approverName = user
    )
}

val approveWithStandardAuthority = handler<ApproveOrderCommand, ApprovalResult> { request ->
    val user = context[USER_CONTEXT_KEY] ?: "unknown"
    println("  [STANDARD] $user approved standard order ${request.orderId}")
    ApprovalResult(
        orderId = request.orderId,
        approved = true,
        approverName = user
    )
}

// Infix operator: creates a fallback chain
val approvalHandler = approveWithHigherAuthority orElse approveWithStandardAuthority

// ── Feature builder with context usage ─────────────────────────────────────

val approvalFeature = feature<ApproveOrderCommand, ApprovalResult> {
    validate(approvalValidator)

    handle { request ->
        // Using RequestContext get operator
        val approver = context[USER_CONTEXT_KEY] ?: "system"
        val level = context[APPROVAL_LEVEL_KEY] ?: "standard"

        println("Processing approval for ${request.orderId} (level: $level, user: $approver)")

        ApprovalResult(
            orderId = request.orderId,
            approved = true,
            approverName = approver
        )
    }
}

// ── Mediator module configuration ──────────────────────────────────────────

// Demonstrates different ways to register handlers:
// 1. Using `add()` function (traditional)
// 2. Using infix `register` operator (concise)
// 3. Using invoke `()` on feature/module (shorthand)

val advancedPatternsModule = mediatorModule {
    // Traditional add() style
    add(approvalHandler)

    // Invoke operator on feature - shorthand for add(feature)
    approvalFeature()
}

// Alternative: Direct module invocation in mediatorK builder
fun getAdvancedModule() = mediatorModule {
    approvalFeature()
    add(approvalHandler)
}
