package com.fajrbahr.mediatork.ksp
import com.fajrbahr.mediatork.handler.*
import com.fajrbahr.mediatork.notification.*

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Modifier

private const val REQUEST_HANDLER_FQN      = "com.fajrbahr.mediatork.RequestHandler"
private const val NOTIFICATION_HANDLER_FQN = "com.fajrbahr.mediatork.NotificationHandler"
private const val PIPELINE_BEHAVIOR_FQN    = "com.fajrbahr.mediatork.PipelineBehavior"
private const val FALLBACK_CHAIN_FQN       = "com.fajrbahr.mediatork.annotations.FallbackChain"

private const val GENERATED_PACKAGE = "com.fajrbahr.mediatork.generated"

class MediatorKProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    // KSP may call process() multiple times (incremental); only run once.
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        invoked = true

        // ── Verify core interfaces are on the classpath ──────────────────────
        val requestHandlerDecl = resolver.getClassDeclarationByName(REQUEST_HANDLER_FQN)
        if (requestHandlerDecl == null) {
            logger.warn("MediatorK KSP: '$REQUEST_HANDLER_FQN' not found — is mediatork on the classpath?")
            return emptyList()
        }

        // ── Collect all concrete classes in the compilation unit ─────────────
        val allClasses = resolver.getAllFiles()
            .flatMap { file -> collectClasses(file.declarations) }
            .filter { cls ->
                cls.classKind == ClassKind.CLASS &&
                Modifier.ABSTRACT !in cls.modifiers &&
                Modifier.SEALED !in cls.modifiers
            }
            .toList()

        // ── Categorise by interface ──────────────────────────────────────────
        val requestHandlers      = allClasses.filter { it.implements(REQUEST_HANDLER_FQN) }
        val notificationHandlers = allClasses.filter { it.implements(NOTIFICATION_HANDLER_FQN) }
        val pipelineBehaviors    = allClasses.filter { it.implements(PIPELINE_BEHAVIOR_FQN) }

        if (requestHandlers.isEmpty() && notificationHandlers.isEmpty() && pipelineBehaviors.isEmpty()) {
            logger.info("MediatorK KSP: no handlers found — nothing to generate.")
            return emptyList()
        }

        // ── Resolve @FallbackChain declarations ──────────────────────────────
        // Map: primary handler FQN → ordered list of all handler FQNs (primary first)
        val fallbackChains = mutableMapOf<String, List<String>>()

        // Set of FQNs that appear as fallbacks (should NOT be registered standalone)
        val fallbackOnlyFqns = mutableSetOf<String>()

        requestHandlers.forEach { cls ->
            val ann = cls.annotations.firstOrNull { ann ->
                ann.annotationType.resolve().declaration.qualifiedName?.asString() == FALLBACK_CHAIN_FQN
            } ?: return@forEach

            val primaryFqn = cls.qualifiedName?.asString() ?: return@forEach

            @Suppress("UNCHECKED_CAST")
            val fallbackTypes = (ann.arguments
                .firstOrNull { it.name?.asString() == "fallbacks" }
                ?.value as? List<*>)
                ?.filterIsInstance<KSType>()
                ?: emptyList()

            val fallbackFqns = fallbackTypes.mapNotNull { it.declaration.qualifiedName?.asString() }

            if (fallbackFqns.isEmpty()) {
                logger.warn("MediatorK KSP: @FallbackChain on '$primaryFqn' has no fallbacks — ignored.")
                return@forEach
            }

            fallbackChains[primaryFqn] = listOf(primaryFqn) + fallbackFqns
            fallbackOnlyFqns += fallbackFqns
        }

        // ── Validate: no class is both a primary and a listed fallback ────────
        fallbackChains.keys.forEach { primaryFqn ->
            if (primaryFqn in fallbackOnlyFqns) {
                logger.error(
                    "MediatorK KSP: '$primaryFqn' is listed as a fallback in another chain " +
                    "but also has its own @FallbackChain — this is ambiguous.",
                )
            }
        }

        // ── Validate: no two standalone handlers for the same request type ────
        val standaloneRequestHandlers = requestHandlers.filter { cls ->
            val fqn = cls.qualifiedName?.asString()
            fqn != null && fqn !in fallbackOnlyFqns
        }

        val requestTypeToHandlers = mutableMapOf<String, MutableList<KSClassDeclaration>>()
        standaloneRequestHandlers.forEach { cls ->
            val args = cls.superTypeArgs(REQUEST_HANDLER_FQN) ?: return@forEach
            val requestTypeFqn = args.firstOrNull()?.type?.resolve()?.declaration
                ?.qualifiedName?.asString() ?: return@forEach
            requestTypeToHandlers.getOrPut(requestTypeFqn) { mutableListOf() }.add(cls)
        }

        requestTypeToHandlers.forEach { (requestType, handlers) ->
            if (handlers.size > 1) {
                val names = handlers.mapNotNull { it.qualifiedName?.asString() }
                logger.error(
                    "MediatorK KSP: Multiple handlers found for '$requestType': $names. " +
                    "Use @FallbackChain on the primary handler to declare an explicit chain, " +
                    "or remove the duplicate.",
                )
            }
        }

        // ── Generate ─────────────────────────────────────────────────────────
        val allSourceFiles = (requestHandlers + notificationHandlers + pipelineBehaviors)
            .mapNotNull { it.containingFile }
            .distinct()

        generateRegistrar(
            standaloneRequestHandlers = standaloneRequestHandlers,
            fallbackChains = fallbackChains,
            notificationHandlers = notificationHandlers,
            pipelineBehaviors = pipelineBehaviors,
            dependencies = Dependencies(aggregating = true, *allSourceFiles.toTypedArray()),
        )

        return emptyList()
    }

    // ── Code generation ───────────────────────────────────────────────────────

    private fun generateRegistrar(
        standaloneRequestHandlers: List<KSClassDeclaration>,
        fallbackChains: Map<String, List<String>>,
        notificationHandlers: List<KSClassDeclaration>,
        pipelineBehaviors: List<KSClassDeclaration>,
        dependencies: Dependencies,
    ) {
        val file = codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = GENERATED_PACKAGE,
            fileName = "GeneratedMediatorRegistrar",
        )

        file.bufferedWriter().use { writer ->
            writer.write(buildString {
                appendLine("// ⚠️  AUTO-GENERATED by MediatorK KSP — do not edit manually.")
                appendLine("// Re-generated on every build. Changes will be overwritten.")
                appendLine()
                appendLine("package $GENERATED_PACKAGE")
                appendLine()
                appendLine("import com.fajrbahr.mediatork.*")
                appendLine()

                // ── GeneratedMediatorRegistrar ─────────────────────────────────
                appendLine("/**")
                appendLine(" * Auto-discovered [MediatorRegistrar].")
                appendLine(" *")
                appendLine(" * Registers:")
                appendLine(" *  - ${standaloneRequestHandlers.size} request handler(s)")
                appendLine(" *  - ${fallbackChains.size} fallback chain(s)")
                appendLine(" *  - ${notificationHandlers.size} notification handler(s)")
                appendLine(" */")
                appendLine("class GeneratedMediatorRegistrar : MediatorRegistrar {")
                appendLine("    override fun register(registry: HandlerRegistry) {")
                appendLine("        registry.scope {")

                // Standalone request handlers (no @FallbackChain)
                standaloneRequestHandlers
                    .filter { it.qualifiedName?.asString() !in fallbackChains.keys }
                    .forEach { cls ->
                        val fqn = cls.qualifiedName?.asString() ?: return@forEach
                        appendLine("            +$fqn()")
                    }

                // Fallback chains
                fallbackChains.forEach { (_, chain) ->
                    val expression = chain.joinToString(" otherwise ") { "$it()" }
                    appendLine("            +($expression)")
                }

                // Notification handlers
                notificationHandlers.forEach { cls ->
                    val fqn = cls.qualifiedName?.asString() ?: return@forEach
                    appendLine("            +$fqn()")
                }

                appendLine("        }")
                appendLine("    }")
                appendLine("}")
                appendLine()

                // ── generatedPipelineBehaviors ─────────────────────────────────
                appendLine("/**")
                appendLine(" * Auto-discovered [PipelineBehavior] instances.")
                appendLine(" *")
                appendLine(" * Pass to [com.fajrbahr.mediatork.MediatorFactory.create] as `pipelineBehaviors`.")
                appendLine(" */")
                appendLine("val generatedPipelineBehaviors: List<PipelineBehavior> = listOf(")
                pipelineBehaviors.forEach { cls ->
                    val fqn = cls.qualifiedName?.asString() ?: return@forEach
                    appendLine("    $fqn(),")
                }
                appendLine(")")
                appendLine()

                // ── Convenience factory ────────────────────────────────────────
                appendLine("/**")
                appendLine(" * Creates a fully wired [Mediator] from all auto-discovered handlers and behaviors.")
                appendLine(" *")
                appendLine(" * Override any parameter to add extra registrars, behaviors, or processors")
                appendLine(" * that are not auto-discovered (e.g. handlers with constructor dependencies).")
                appendLine(" */")
                appendLine("fun createMediator(")
                appendLine("    extraRegistrars: List<MediatorRegistrar> = emptyList(),")
                appendLine("    extraBehaviors: List<PipelineBehavior> = emptyList(),")
                appendLine("    preProcessors: List<RequestPreProcessor> = emptyList(),")
                appendLine("    postProcessors: List<RequestPostProcessor> = emptyList(),")
                appendLine("    notificationPublisher: NotificationPublisher = ParallelNotificationPublisher(),")
                appendLine("    verifyHandlers: Boolean = true,")
                appendLine("): Mediator = MediatorFactory.create(")
                appendLine("    registrars = listOf(GeneratedMediatorRegistrar()) + extraRegistrars,")
                appendLine("    pipelineBehaviors = generatedPipelineBehaviors + extraBehaviors,")
                appendLine("    preProcessors = preProcessors,")
                appendLine("    postProcessors = postProcessors,")
                appendLine("    notificationPublisher = notificationPublisher,")
                appendLine("    verifyHandlers = verifyHandlers,")
                appendLine(")")
            })
        }

        logger.info(
            "MediatorK KSP: generated GeneratedMediatorRegistrar — " +
            "${standaloneRequestHandlers.size} request, " +
            "${fallbackChains.size} chain(s), " +
            "${notificationHandlers.size} notification, " +
            "${pipelineBehaviors.size} pipeline behavior(s).",
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Recursively collects all [KSClassDeclaration]s from a sequence of declarations. */
    private fun collectClasses(declarations: Sequence<KSDeclaration>): Sequence<KSClassDeclaration> =
        declarations.filterIsInstance<KSClassDeclaration>().flatMap { cls ->
            sequenceOf(cls) + collectClasses(cls.declarations)
        }

    /**
     * Returns `true` if this class directly or indirectly implements the interface
     * identified by [interfaceFqn].
     */
    private fun KSClassDeclaration.implements(interfaceFqn: String): Boolean =
        superTypes.any { ref ->
            val resolved = ref.resolve()
            val declFqn = resolved.declaration.qualifiedName?.asString()
            declFqn == interfaceFqn ||
            (resolved.declaration as? KSClassDeclaration)?.implements(interfaceFqn) == true
        }

    /**
     * Returns the type arguments of the **direct** supertype matching [interfaceFqn],
     * or `null` if this class does not directly implement that interface.
     *
     * Example: for `class Foo : RequestHandler<PlaceOrder, OrderId>` and
     * [interfaceFqn] = `"...RequestHandler"`, returns `[PlaceOrder, OrderId]`.
     */
    private fun KSClassDeclaration.superTypeArgs(interfaceFqn: String): List<KSTypeArgument>? {
        for (ref in superTypes) {
            val resolved = ref.resolve()
            if (resolved.declaration.qualifiedName?.asString() == interfaceFqn) {
                return resolved.arguments
            }
            // One level of indirection (e.g. abstract base class)
            val indirect = (resolved.declaration as? KSClassDeclaration)?.superTypeArgs(interfaceFqn)
            if (indirect != null) return indirect
        }
        return null
    }
}
