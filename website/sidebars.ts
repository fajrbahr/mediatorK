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
                'core/pipeline',
                'core/processors',
                'core/context',
                'core/exceptions',
                'core/validation',
                'core/factory',
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
            label: 'Examples',
            collapsed: false,
            items: ['examples/basic', 'examples/spring-boot-3'],
        },
        {
            type: 'category',
            label: 'Testing',
            collapsed: false,
            items: ['testing/before-mediator', 'testing/dump-mediator', 'testing/fake-mediator', 'testing/notification-testing', 'testing/handler-testing', 'testing/viewmodel-testing', 'testing/handler-validation'],
        },
        'api',
        'about',
        'resources',
        'sample',
    ],
};

export default sidebars;
