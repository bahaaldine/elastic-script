import type { CoreSetup, CoreStart, Plugin, AppMountParameters } from '@kbn/core/public';

export interface MoltlerPluginSetup {}
export interface MoltlerPluginStart {}

export class MoltlerPlugin implements Plugin<MoltlerPluginSetup, MoltlerPluginStart> {
  public setup(core: CoreSetup): MoltlerPluginSetup {
    core.application.register({
      id: 'moltler',
      title: 'Moltler',
      async mount(params: AppMountParameters) {
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
