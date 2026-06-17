package sample.validation

import com.fajrbahr.mediatork.validator.ValidationResult


/**
 * Validates email addresses using a standard regex pattern.
 *
 * This validator:
 * - Rejects empty or blank strings
 * - Requires local-part, '@' symbol, and domain
 * - Allows dots in local-part (but not at start or end, and not consecutive)
 * - Allows common TLDs and subdomains
 *
 * Example usage:
 * ```kotlin
 * if (EmailValidator.isValid("user@example.com")) { ... }
 * val result = EmailValidator.validate("invalid")
 * ```
 */
interface Validator<T> {
    fun isValid(input: T): Boolean
    fun validate(input: T): ValidationResult
}

object EmailValidator : Validator<String> {

    // Regex pattern that covers most valid email formats:
    // - Local part: letters, digits, dots, +, -, _ (dots not at start/end, no consecutive dots)
    // - Domain: letters, digits, hyphens, dots (TLD at least 2 letters)
    // - Total length up to 254 characters (RFC 5321)
    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    )

    override fun isValid(input: String): Boolean {
        if (input.isBlank()) return false
        // Basic length sanity (RFC 5321: 254 chars max)
        if (input.length > 254) return false
        return EMAIL_REGEX.matches(input.trim())
    }

    override fun validate(input: String): ValidationResult = if (isValid(input)) {
        ValidationResult.Success
    } else {
        ValidationResult.error("Invalid email format")
    }
}