package com.fajrbahr.mediatork.ksp.koin

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class MediatorKKoinProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        MediatorKKoinProcessor(environment.codeGenerator, environment.logger)
}
