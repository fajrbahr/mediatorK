import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
    docsSidebar: [
        {
            type: 'category',
            label: 'Getting Started',
            collapsed: false,
            items: ['intro', 'the-promise', 'vertical-slice', 'installation'],
        },
        {
            type: 'category',
            label: 'Core Concepts',
            collapsed: false,
            items: [
                'core/requests',
                'core/notifications',
                {
                    type: 'category',
                    label: 'Pipeline Behaviors',
                    link: {type: 'doc', id: 'core/pipeline'},
                    collapsed: false,
                    items: ['core/built-in-behaviors'],
                },
                'core/processors',
                'core/context',
                'core/fallback',
                'core/exceptions',
                'core/validation',
                'core/factory',
                'core/ab-testing',
            ],
        },
        {
            type: 'category',
            label: 'Integration',
            collapsed: false,
            items: [
                'integration/jvm',
                'integration/viewmodel',
                'integration/kmp',
                'integration/ktor',
                'integration/spring',
                'integration/koin',
            ],
        },
        {
            type: 'category',
            label: 'Samples',
            collapsed: false,
            items: ['examples/basic', 'examples/spring-boot-3', 'sample'],
        },
        {
            type: 'category',
            label: 'Testing',
            collapsed: false,
            items: ['testing/before-mediator', 'testing/dump-mediator', 'testing/fake-mediator', 'testing/spy', 'testing/notification-testing', 'testing/handler-testing', 'testing/viewmodel-testing', 'testing/handler-validation'],
        },
        'api',
        'about',
        'resources',
    ],
};

export default sidebars;
