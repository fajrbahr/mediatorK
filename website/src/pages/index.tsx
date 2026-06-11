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

const FEATURES = [
    {
        icon: '⚡',
        title: 'Coroutine-native',
        desc: 'suspend all the way down — no callbacks, no blocking wrappers.',
    },
    {
        icon: '🧩',
        title: 'KMP ready',
        desc: 'Single commonMain dependency. Works on JVM, Android, and iOS.',
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
        desc: 'Test ViewModels without a mock library — swap real handlers for fakes.',
    },
    {
        icon: '🤖',
        title: 'Android ViewModel Testing',
        desc: 'Test Android ViewModels with zero mocking library — inject fake handlers directly.',
    },
    {
        icon: '🛡️',
        title: 'Pipeline behaviors',
        desc: 'Compose cross-cutting concerns: logging, retry, auth, validation.',
    },
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
                        <img src="img/mediator-day.png" alt="MediatorK routing requests"
                             className={styles.sectionImage}/>
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
                        <div className={styles.platformGrid}>
                            {[
                                ['JVM', 'Spring Boot · Ktor · CLI'],
                                ['Android', 'androidTarget'],
                                ['iOS Device', 'iosArm64'],
                                ['iOS Simulator', 'iosSimulatorArm64 · iosX64'],
                            ].map(([platform, detail]) => (
                                <div key={platform} className={styles.platformCard}>
                                    <div className={styles.platformName}>{platform}</div>
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
                        <Link className="button button--primary button--lg" to="/docs/installation">
                            View Installation Guide →
                        </Link>
                    </div>
                </section>
            </main>
        </Layout>
    );
}
