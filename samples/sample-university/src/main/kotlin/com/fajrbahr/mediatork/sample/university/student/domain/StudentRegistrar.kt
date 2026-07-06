package com.fajrbahr.mediatork.sample.university.student.domain

// Note: University sample registrar pattern will be refactored to use new buildMediatorK
// DSL when gradle version is updated to 0.7.2. For now, keeping original pattern.
//
// The domain handler/feature functions have been updated to demonstrate new chaining:
// - handle { ... }.retry(n).timeout(...).measure()
// - Query handlers with .cache(keyFrom = { ... }).timeout(...)
