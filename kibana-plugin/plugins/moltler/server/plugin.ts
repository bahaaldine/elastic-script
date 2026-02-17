import type {
  PluginInitializerContext,
  CoreSetup,
  CoreStart,
  Plugin,
  Logger,
} from '@kbn/core/server';

import type { MoltlerPluginSetup, MoltlerPluginStart } from './types';

export class MoltlerPlugin implements Plugin<MoltlerPluginSetup, MoltlerPluginStart> {
  private readonly logger: Logger;

  constructor(initializerContext: PluginInitializerContext) {
    this.logger = initializerContext.logger.get();
  }

  public setup(core: CoreSetup): MoltlerPluginSetup {
    this.logger.info('Moltler plugin setup');
    return {};
  }

  public start(core: CoreStart): MoltlerPluginStart {
    this.logger.info('Moltler plugin started');
    return {};
  }

  public stop() {}
}
