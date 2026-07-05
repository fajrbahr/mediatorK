@file:Suppress("SpreadOperator", "ReturnCount", "LongMethod")

package com.fajrbahr.mediatork.ksp.koin

import com.fajrbahr.mediatork.ksp.koin.MediatorKKoinProcessor.Companion.EXCLUDE_ANNOTATION
import com.fajrbahr.mediatork.ksp.koin.MediatorKKoinProcessor.Companion.NOTIFICATION_HANDLER
import com.fajrbahr.mediatork.ksp.koin.MediatorKKoinProcessor.Companion.REQUEST_HANDLER
import com.fajrbahr.mediatork.ksp.koin.MediatorKKoinProcessor.Companion.STREAM_REQUEST_HANDLER
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * KSP processor that discovers concrete MediatorK handler implementations and
 * generates a `GeneratedMediatorRegistrar` plus a Koin module
 * (`generatedHandlersModule`) declaring each handler as a singleton.
 *
 * Discovered handler kinds (matched by fully-qualified interface name, walking
 * the whole supertype hierarchy so handlers extending abstract base classes are
 * found too):
 * - [REQUEST_HANDLER] — registered via the registry `+` DSL as request handlers.
 * - [STREAM_REQUEST_HANDLER] — registered as stream request handlers.
 * - [NOTIFICATION_HANDLER] — registered as notification handlers.
 *
 * Skipped declarations:
 * - abstract and sealed classes, interfaces, objects, enums (only concrete
 *   top-level `class` declarations with an invokable constructor are eligible
 *   for `singleOf(::Handler)`),
 * - classes annotated with [EXCLUDE_ANNOTATION], which opts a handler out of
 *   generated registration so it can be wired manually,
 * - declarations in test sources.
 */
class MediatorKKoinProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var processed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // The generator aggregates over all files at once; run on the first round only.
        if (processed) return emptyList()
        processed = true

        val requestHandlers = resolver.findConcreteImplementations(REQUEST_HANDLER)
        val streamHandlers = resolver.findConcreteImplementations(STREAM_REQUEST_HANDLER)
        val notificationHandlers = resolver.findConcreteImplementations(NOTIFICATION_HANDLER)

        if (requestHandlers.isEmpty() && streamHandlers.isEmpty() && notificationHandlers.isEmpty()) {
            logger.info("MediatorK Koin KSP: no handlers found")
            return emptyList()
        }

        logger.info(
            "MediatorK Koin KSP: found ${requestHandlers.size} request handlers, " +
                    "${streamHandlers.size} stream request handlers, " +
                    "${notificationHandlers.size} notification handlers"
        )

        generateKoinModule(requestHandlers, streamHandlers, notificationHandlers)

        return emptyList()
    }

    /**
     * Returns all concrete, non-excluded top-level classes in the compilation
     * that implement [interfaceQualifiedName] anywhere in their supertype hierarchy.
     */
    private fun Resolver.findConcreteImplementations(interfaceQualifiedName: String): List<KSClassDeclaration> =
        getAllFiles()
            .filter { file -> !isTestFile(file.filePath) }
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { cls ->
                cls.classKind == ClassKind.CLASS &&
                        !cls.modifiers.contains(Modifier.ABSTRACT) &&
                        !cls.modifiers.contains(Modifier.SEALED) &&
                        !cls.isExcluded() &&
                        cls.implementsInterface(interfaceQualifiedName)
            }
            .toList()

    /** Returns `true` if the class is annotated with [EXCLUDE_ANNOTATION]. */
    private fun KSClassDeclaration.isExcluded(): Boolean =
        annotations.any { annotation ->
            // Cheap short-name check first; resolve only on a potential match.
            annotation.shortName.asString() == EXCLUDE_ANNOTATION_SIMPLE_NAME &&
                    annotation.annotationType.resolve()
                        .declaration.qualifiedName?.asString() == EXCLUDE_ANNOTATION
        }

    /**
     * Walks the supertype hierarchy (interfaces and superclasses, transitively)
     * looking for [interfaceQualifiedName]. Handles handlers that implement the
     * interface indirectly, e.g. via an abstract base handler class.
     */
    private fun KSClassDeclaration.implementsInterface(
        interfaceQualifiedName: String,
        visited: MutableSet<String> = mutableSetOf(),
    ): Boolean = superTypes.any { superTypeRef ->
        val declaration = superTypeRef.resolve().declaration as? KSClassDeclaration
            ?: return@any false
        val qualifiedName = declaration.qualifiedName?.asString() ?: return@any false
        when {
            qualifiedName == interfaceQualifiedName -> true
            !visited.add(qualifiedName) -> false // already checked this branch
            else -> declaration.implementsInterface(interfaceQualifiedName, visited)
        }
    }

    private fun isTestFile(path: String): Boolean =
        path.contains("/test/") || path.contains("Test.kt") || path.contains("Spec.kt")

    private fun generateKoinModule(
        requestHandlers: List<KSClassDeclaration>,
        streamHandlers: List<KSClassDeclaration>,
        notificationHandlers: List<KSClassDeclaration>,
    ) {
        // A class could implement more than one handler interface; register it once.
        val allHandlers = (requestHandlers + streamHandlers + notificationHandlers)
            .distinctBy { it.qualifiedName?.asString() ?: it.simpleName.asString() }
            .sortedBy { it.simpleName.asString() }

        // The generated code refers to handlers by simple name (imports + `::Name`
        // constructor references), so two handlers with the same simple name in
        // different packages cannot be registered together.
        allHandlers
            .groupBy { it.simpleName.asString() }
            .filterValues { it.size > 1 }
            .forEach { (name, clashes) ->
                logger.error(
                    "MediatorK Koin KSP: multiple handlers share the simple name '$name': " +
                            clashes.mapNotNull { it.qualifiedName?.asString() }.joinToString() +
                            ". Rename one, or annotate one with @ExcludeFromGeneratedRegistrar " +
                            "and register it manually."
                )
            }

        val allFiles = allHandlers.mapNotNull { it.containingFile }.toTypedArray()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *allFiles),
            packageName = GENERATED_PACKAGE,
            fileName = "GeneratedKoinMediatorModule",
        )

        file.bufferedWriter().use { writer ->
            writer.write(buildString {
                appendLine("package $GENERATED_PACKAGE")
                appendLine()
                appendLine("// Auto-generated by mediatork-ksp-koin. Do not edit.")
                appendLine()
                appendLine("import com.fajrbahr.mediatork.HandlerRegistry")
                appendLine("import com.fajrbahr.mediatork.api.MediatorRegistrar")
                appendLine("import org.koin.core.module.dsl.singleOf")
                appendLine("import org.koin.dsl.bind")
                appendLine("import org.koin.dsl.module")

                val imports = allHandlers
                    .map { "${it.packageName.asString()}.${it.simpleName.asString()}" }
                    .distinct()
                    .sorted()
                imports.forEach { appendLine("import $it") }

                appendLine()

                appendLine("class GeneratedMediatorRegistrar(")
                allHandlers.forEachIndexed { i, handler ->
                    val name = handler.simpleName.asString()
                    val prop = name.replaceFirstChar { it.lowercaseChar() }
                    val comma = if (i < allHandlers.lastIndex) "," else ""
                    appendLine("    private val $prop: $name$comma")
                }
                appendLine(") : MediatorRegistrar {")
                appendLine("    override fun register(registry: HandlerRegistry) {")
                appendLine("        registry.apply {")
                allHandlers.forEach { handler ->
                    val prop = handler.simpleName.asString().replaceFirstChar { it.lowercaseChar() }
                    appendLine("            register($prop)")
                }
                appendLine("        }")
                appendLine("    }")
                appendLine("}")
                appendLine()

                appendLine("val generatedHandlersModule = module {")
                allHandlers.forEach { handler ->
                    appendLine("    singleOf(::${handler.simpleName.asString()})")
                }
                appendLine("    singleOf(::GeneratedMediatorRegistrar) bind MediatorRegistrar::class")
                appendLine("}")
            })
        }
    }

    private companion object {
        const val REQUEST_HANDLER = "com.fajrbahr.mediatork.api.RequestHandler"
        const val STREAM_REQUEST_HANDLER = "com.fajrbahr.mediatork.api.StreamRequestHandler"
        const val NOTIFICATION_HANDLER = "com.fajrbahr.mediatork.api.NotificationHandler"
        const val EXCLUDE_ANNOTATION = "com.fajrbahr.mediatork.annotations.ExcludeFromGeneratedRegistrar"
        const val EXCLUDE_ANNOTATION_SIMPLE_NAME = "ExcludeFromGeneratedRegistrar"
        const val GENERATED_PACKAGE = "com.fajrbahr.mediatork.generated"
    }
}
