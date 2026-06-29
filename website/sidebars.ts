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
                    items: ['core/free-aop', 'core/built-in-behaviors', 'core/processors'],
                },
                'core/validation',
                'core/factory',
                'core/ab-testing',
            ],
        },
        {
            type: 'category',
            label: 'Advanced',
            collapsed: false,
            items: [
                'core/context',
                'core/exceptions',
                'core/stream',
                'core/fallback',
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
        'sample',
        {
            type: 'category',
            label: 'Testing',
            collapsed: false,
            items: ['testing/before-mediator', 'testing/dump-mediator', 'testing/fake-mediator', 'testing/spy', 'testing/notification-testing', 'testing/handler-testing', 'testing/viewmodel-testing', 'testing/handler-validation'],
        },
        'ai-prompts',
        'about',
        'resources',
    ],
};

export default sidebars;
