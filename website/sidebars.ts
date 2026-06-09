import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: ['intro', 'installation'],
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
        'integration/kmp',
        'integration/spring',
        'integration/ktor',
        'integration/koin',
        'integration/viewmodel',
      ],
    },
    {
      type: 'category',
      label: 'Examples',
      collapsed: false,
      items: ['examples/basic', 'examples/spring-boot-3'],
    },
    'testing',
    'api',
  ],
};

export default sidebars;
