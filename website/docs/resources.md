---
id: resources
title: Resources
sidebar_label: Resources
---

# Resources

## Vertical Slice Architecture: Article Series

A deep-dive series on moving from layered architecture to Vertical Slices, written by the creator of MediatorK:

- [Part 1: From the Layered Architecture Trap to Vertical Slices](https://www.linkedin.com/pulse/from-layered-architecture-trap-vertical-slices-part-1-alfararjeh-senff/)
- [Part 2: From the Layered Architecture Trap to Vertical Slices](https://www.linkedin.com/pulse/from-layered-architecture-trap-vertical-slices-part-2-alfararjeh-p0gnf/)
- [Part 3: From the Layered Architecture Trap to Vertical Slices](https://www.linkedin.com/pulse/from-layered-architecture-trap-vertical-slices-part-3-alfararjeh-ys6ef/)
- [Part 4: From the Layered Architecture Trap to Vertical Slices](https://www.linkedin.com/pulse/from-layered-architecture-trap-vertical-slices-part-4-alfararjeh-hbfof/)
- [Part 5: From the Layered Architecture Trap to Vertical Slices](https://www.linkedin.com/pulse/from-layered-architecture-trap-vertical-slices-part-4-alfararjeh-78tyf/)
- [Depend on Abstractions, Not Concretions](https://www.linkedin.com/pulse/depend-abstractions-concretions-huthayfah-alfararjeh-hwgvf/)

---

## Video

**Jimmy Bogard: Vertical Slice Architecture**

The talk that inspired MediatorK; Jimmy Bogard explains why Vertical Slices are a better default than layered
architecture.

[Watch on YouTube →](https://www.youtube.com/watch?v=oAoaMlS1PWo)

---

## MediatR (.NET)

MediatorK is inspired by [MediatR](https://github.com/LuckyPennySoftware/MediatR), the most widely-used mediator
library in the .NET ecosystem, by Jimmy Bogard.

---

## Testing

**Never mock in unit tests.**

Unit tests should only verify pure functions. If your code is hard to test without mocking, that's a signal; extract
the pure logic and test that directly. Everything else belongs in integration or end-to-end tests.

The testing pyramid puts unit tests at ~70% of your test suite for a reason: they're fast, stable, and reliable. Mocking
undermines all of that; it couples your tests to implementation details, makes refactoring painful, and gives you false
confidence. Mock libraries make this worse by making mocking feel easy.

- [Mocking is a Code Smell](https://medium.com/javascript-scene/mocking-is-a-code-smell-944a70c90a6a), by Eric Elliott
