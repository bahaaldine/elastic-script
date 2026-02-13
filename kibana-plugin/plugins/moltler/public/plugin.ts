import type { CoreSetup, CoreStart, Plugin, AppMountParameters } from '@kbn/core/public';
import { PLUGIN_ID, PLUGIN_NAME } from '../common';

export interface MoltlerPluginSetup {}
export interface MoltlerPluginStart {}

export class MoltlerPlugin implements Plugin<MoltlerPluginSetup, MoltlerPluginStart> {
  public setup(core: CoreSetup): MoltlerPluginSetup {
    // Register the application
    core.application.register({
      id: PLUGIN_ID,
      title: PLUGIN_NAME,
      // Icon for the nav menu - using a deer/elk icon to match Moltler branding
      euiIconType: 'logstashInput',
      category: {
        id: 'management',
        label: 'Management',
        order: 5000,
      },
      async mount(params: AppMountParameters) {
        // Load application bundle
        const { renderApp } = await import('./application');
        const [coreStart] = await core.getStartServices();
        return renderApp(coreStart, params);
      },
    });

    return {};
  }

  public start(core: CoreStart): MoltlerPluginStart {
    return {};
  }

  public stop() {}
}
