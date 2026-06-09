import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
    title: 'MediatorK',
    tagline: 'Coroutine-first Mediator for Kotlin & KMP',
    favicon: 'img/favicon.png',

    future: {
        v4: true,
    },

    url: 'https://fajrbahr.github.io',
    baseUrl: '/mediatorK/',

    organizationName: 'fajrbahr',
    projectName: 'MediatorK',

    onBrokenLinks: 'warn',
    onBrokenMarkdownLinks: 'warn',

    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    presets: [
        [
            'classic',
            {
                docs: {
                    sidebarPath: './sidebars.ts',
                    editUrl: 'https://github.com/fajrbahr/MediatorK/tree/main/website/',
                    routeBasePath: 'docs',
                },
                blog: false,
                theme: {
                    customCss: './src/css/custom.css',
                },
            } satisfies Preset.Options,
        ],
    ],

    themeConfig: {
        colorMode: {
            defaultMode: 'dark',
            disableSwitch: true,
            respectPrefersColorScheme: false,
        },
        navbar: {
            title: 'MediatorK',
            logo: {
                alt: 'MediatorK Logo',
                src: 'img/mediator-logo.png',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'docsSidebar',
                    position: 'left',
                    label: 'Docs',
                },
                {
                    to: '/docs/api',
                    label: 'API',
                    position: 'left',
                },
                {
                    to: '/docs/license',
                    label: 'License',
                    position: 'right',
                },
                {
                    href: 'https://github.com/fajrbahr/MediatorK',
                    label: 'GitHub',
                    position: 'right',
                },
                {
                    href: 'https://central.sonatype.com/artifact/io.github.fajrbahr/mediatork',
                    label: 'Maven Central',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: 'dark',
            links: [
                {
                    title: 'Documentation',
                    items: [
                        {label: 'Introduction', to: '/docs/intro'},
                        {label: 'Installation', to: '/docs/installation'},
                        {label: 'API Reference', to: '/docs/api'},
                    ],
                },
                {
                    title: 'Integrations',
                    items: [
                        {label: 'Spring Boot', to: '/docs/integration/spring'},
                        {label: 'Koin', to: '/docs/integration/koin'},
                        {label: 'Kotlin Multiplatform', to: '/docs/integration/kmp'},
                    ],
                },
                {
                    title: 'Project',
                    items: [
                        {label: 'GitHub', href: 'https://github.com/fajrbahr/MediatorK'},
                        {label: 'Releases', href: 'https://github.com/fajrbahr/MediatorK/releases'},
                        {label: 'Issues', href: 'https://github.com/fajrbahr/MediatorK/issues'},
                    ],
                },
                {
                    title: 'Creator',
                    items: [
                        {label: 'Huzaifa Al-Fararjeh', to: '/docs/about'},
                        {label: 'LinkedIn', href: 'https://www.linkedin.com/in/hfararjeh/'},
                        {label: 'hfararjeh28@gmail.com', href: 'mailto:hfararjeh28@gmail.com'},
                    ],
                },
            ],
            copyright: `© ${new Date().getFullYear()} Huzaifa Al-Fararjeh. Built with Docusaurus.`,
        },
        prism: {
            theme: prismThemes.vsDark,
            darkTheme: prismThemes.vsDark,
            additionalLanguages: ['kotlin', 'groovy', 'bash', 'toml'],
        },
        docs: {
            sidebar: {
                hideable: true,
                autoCollapseCategories: false,
            },
        },
    } satisfies Preset.ThemeConfig,
};

export default config;
