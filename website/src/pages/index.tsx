import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import CodeBlock from '@theme/CodeBlock';

import styles from './index.module.css';

const QUICK_EXAMPLE = `// 1. Define a request
data class GetUserQuery(val id: String) : Request<User>

// 2. Implement a handler
class GetUserHandler(private val db: UserRepository)
  : RequestHandler<GetUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetUserQuery,
    ): User = db.findById(request.id) ?: error("Not found")
}

// 3. Wire it up & use it
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar(db)),
)
val user: User = mediator.send(GetUserQuery("user-1"))`;

const MOCK_BEFORE_CODE = `@Test
fun \`place order - notifies user on success\`() {
    val notificationService = mockk<NotificationService>()
    val inventoryRepo = mockk<InventoryRepository>()
    val orderRepo = mockk<OrderRepository>()
    val paymentGateway = mockk<PaymentGateway>()
    val emailSender = mockk<EmailSender>()
    // … 8 more mocks …

    every { inventoryRepo.reserve(any()) } returns true
    every { orderRepo.save(any()) } returns order
    every { paymentGateway.charge(any()) } returns receipt
    every { notificationService.notify(any()) } just Runs
    // … 10+ more stubs …

    val vm = OrderViewModel(
        notificationService, inventoryRepo,
        orderRepo, paymentGateway, emailSender, …
    )
    vm.placeOrder(cart)

    verify { notificationService.notify(match { it.type == "ORDER_PLACED" }) }
}`;

const MOCK_AFTER_CODE = `@Test
fun \`place order - notifies user on success\`() {
    val fakeNotify = FakePlaceOrderHandler(shouldNotify = true)
    val mediator = TestMediator(fakeNotify)

    val vm = OrderViewModel(mediator)
    vm.placeOrder(cart)

    assertTrue(fakeNotify.notified)
}

// A simple fake — no mocking library needed
class FakePlaceOrderHandler(val shouldNotify: Boolean)
  : RequestHandler<PlaceOrderCommand, OrderResult> {
    var notified = false
    override suspend fun handle(...): OrderResult {
        if (shouldNotify) notified = true
        return OrderResult.Success
    }
}`;

const BEFORE_CODE = `class InitialViewModel(
    private val applicationMetadata: ApplicationMetadata,
    private val retrieveAndStoreTogglesUseCase: RetrieveAndStoreTogglesUseCase,
    watchTogglesUseCase: WatchTogglesUseCase,
    private val persistCachedInfoUseCase: PersistCachedInfoUseCase,
    private val fetchActiveUserAndStoreUseCase: FetchActiveUserAndStoreUseCase,
    fetchPreferredLocaleUseCase: FetchPreferredLocaleUseCase,
    fetchVisualThemeUseCase: FetchVisualThemeUseCase,
    private val metricsReporterPort: MetricsReporterPort,
    val runtimeSettings: RuntimeSettings,
    val speedMonitor: SpeedMonitor,
    val cloudPerformanceTracker: PerformanceTraceListener,
    val simpleLoggingTracker: SimpleLoggingTracker,
) : ViewModel()`;

const AFTER_CODE = `class InitialViewModel(
    private val mediator: Mediator,
) : ViewModel()`;

const FEATURES = [
    {
        icon: '⚡',
        title: 'Coroutine-native',
        desc: 'suspend all the way down — no callbacks, no blocking wrappers.',
    },
    {
        icon: '🧩',
        title: 'KMP ready',
        desc: 'Single commonMain dependency. Works on JVM, Android, iOS, and more.',
    },
    {
        icon: '🔌',
        title: 'Framework-agnostic',
        desc: 'Spring Boot, Ktor, Koin, or plain Kotlin — all first-class.',
    },
    {
        icon: '🪶',
        title: 'Zero magic',
        desc: 'No kotlin-reflect, no annotation processors, no code generation.',
    },
    {
        icon: '🧪',
        title: 'Testable by design',
        desc: 'ViewModel tests need zero mocking — swap real handlers for fakes.',
    },
    {
        icon: '🛡️',
        title: 'Pipeline behaviors',
        desc: 'Compose cross-cutting concerns: logging, retry, auth, circuit-breaker.',
    },
];

const PLATFORMS = [
    {name: 'JVM', detail: 'Spring Boot · Ktor · CLI'},
    {name: 'Android', detail: 'androidTarget · native'},
    {name: 'iOS', detail: 'iosArm64 · iosSimulatorArm64 · iosX64'},
    {name: 'macOS', detail: 'macosArm64 · macosX64'},
    {name: 'tvOS', detail: 'tvosArm64 · tvosSimulatorArm64 · tvosX64'},
    {name: 'watchOS', detail: 'watchosArm32/64 · watchosSimulatorArm64'},
    {name: 'Linux', detail: 'linuxArm64 · linuxX64'},
    {name: 'Web / Wasm', detail: 'js · wasmJs · wasmWasi'},
];

export default function Home(): ReactNode {
    return (
        <Layout
            title="MediatorK — Coroutine-first Mediator for Kotlin"
            description="A coroutine-first Mediator library for Kotlin and Kotlin Multiplatform implementing CQRS and Vertical Slice patterns."
        >
            {/* Hero */}
            <header className={clsx('hero hero--primary', styles.heroBanner)}>
                <div className="container">
                    <img src="img/mediator-logo.png" alt="MediatorK" className={styles.heroLogo}/>
                    <h1 className="hero__title">MediatorK</h1>
                    <p className="hero__subtitle">
                        Coroutine-first Mediator for Kotlin &amp; Kotlin Multiplatform
                    </p>
                    <p className={styles.heroMeta}>
                        CQRS &middot; Vertical Slice &middot; No reflection &middot; No code-gen
                    </p>
                    <div className={styles.badgeRow}>
                        <img
                            src="https://img.shields.io/maven-central/v/io.github.fajrbahr/mediatork?color=a97cf8&label=Maven%20Central"
                            alt="Maven Central"/>
                        <img src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white"
                             alt="Kotlin 2.0+"/>
                        <img src="https://img.shields.io/badge/KMP-ready-60a5fa" alt="KMP ready"/>
                        <img src="https://img.shields.io/badge/license-CC0%201.0-34d399" alt="CC0 1.0"/>
                    </div>
                    <div className={styles.buttons}>
                        <Link className="button button--primary button--lg" to="/docs/intro">
                            Get Started
                        </Link>
                        <Link className="button button--secondary button--lg" to="/docs/installation">
                            Installation
                        </Link>
                    </div>
                </div>
            </header>

            <main>
                {/* The promise — before / after */}
                <section className={styles.promiseSection}>
                    <div className="container">
                        <h2 className={styles.sectionTitle}>The Promise</h2>
                        <p className={styles.sectionSub}>
                            From a ViewModel with 10+ constructor parameters — down to one.
                        </p>
                        <div className={styles.beforeAfterGrid}>
                            <div className={styles.beforeAfterCard}>
                                <div className={clsx(styles.beforeAfterLabel, styles.beforeLabel)}>Before</div>
                                <CodeBlock language="kotlin">{BEFORE_CODE}</CodeBlock>
                            </div>
                            <div className={styles.beforeAfterCard}>
                                <div className={clsx(styles.beforeAfterLabel, styles.afterLabel)}>After</div>
                                <CodeBlock language="kotlin">{AFTER_CODE}</CodeBlock>
                                <p className={styles.afterNote}>
                                    Every action becomes <code>mediator.send(...)</code>. Each use-case moves
                                    into a focused handler — testable in isolation, no mocking library needed.
                                </p>
                            </div>
                        </div>
                        <div className={styles.promiseCta}>
                            <Link className="button button--outline button--primary" to="/docs/the-promise">
                                See the full story →
                            </Link>
                        </div>
                    </div>
                </section>

                {/* Hello Mocking */}
                <section className={styles.mockingSection}>
                    <div className="container">
                        <h2 className={styles.sectionTitle}>zero mocking setup</h2>
                        <p className={styles.sectionSub}>
                            No mocking library. No 20-line setup. Just a plain fake handler.
                        </p>
                        <div className={styles.beforeAfterGrid}>
                            <div className={styles.beforeAfterCard}>
                                <div className={clsx(styles.beforeAfterLabel, styles.beforeLabel)}>Before — Mockk hell
                                </div>
                                <CodeBlock language="kotlin">{MOCK_BEFORE_CODE}</CodeBlock>
                            </div>
                            <div className={styles.beforeAfterCard}>
                                <div className={clsx(styles.beforeAfterLabel, styles.afterLabel)}>After — Just a fake
                                </div>
                                <CodeBlock language="kotlin">{MOCK_AFTER_CODE}</CodeBlock>
                                <p className={styles.afterNote}>
                                    Each handler is a pure function. Swap it with a fake object in tests —
                                    no mocking framework, no <code>every/verify</code> incantations.
                                </p>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Quick example */}
                <section className={styles.quickExample}>
                    <div className="container">
                        <h2 className={styles.sectionTitle}>Quick Example</h2>
                        <p className={styles.sectionSub}>
                            Three steps: define a request, implement a handler, send it.
                        </p>
                        <div className={styles.codeWrapper}>
                            <CodeBlock language="kotlin">{QUICK_EXAMPLE}</CodeBlock>
                        </div>
                        <div className={styles.imageRow}>
<img src="img/mediator-day.png" alt="MediatorK routing requests"
                                 className={styles.sectionImage}/>
                        </div>
                    </div>
                </section>

                {/* Features */}
                <section className={styles.featuresSection}>
                    <div className="container">
                        <h2 className={styles.sectionTitle}>Why MediatorK?</h2>
                        <div className={styles.featuresGrid}>
                            {FEATURES.map(({icon, title, desc}) => (
                                <div key={title} className={styles.featureCard}>
                                    <div className={styles.featureIcon}>{icon}</div>
                                    <div className={styles.featureTitle}>{title}</div>
                                    <div className={styles.featureDesc}>{desc}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                </section>

                {/* Platforms */}
                <section className={styles.platformSection}>
                    <div className="container">
                        <h2 className={styles.sectionTitle}>Supported Platforms</h2>
                        <p className={styles.sectionSub}>
                            All APIs live in <code>commonMain</code> — one dependency, every target.
                        </p>
                        <div className={styles.platformGrid}>
                            {PLATFORMS.map(({name, detail}) => (
                                <div key={name} className={styles.platformCard}>
                                    <div className={styles.platformName}>{name}</div>
                                    <div className={styles.platformDetail}>{detail}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                </section>

                {/* CTA */}
                <section className={styles.ctaSection}>
                    <div className="container">
                        <h2>Ready to build?</h2>
                        <p>Add MediatorK to your project in under a minute.</p>
                        <div className={styles.buttons}>
                            <Link className="button button--primary button--lg" to="/docs/installation">
                                View Installation Guide →
                            </Link>
                            <Link className="button button--secondary button--lg" to="/docs/sample">
                                See Samples →
                            </Link>
                        </div>
                    </div>
                </section>
            </main>
        </Layout>
    );
}
