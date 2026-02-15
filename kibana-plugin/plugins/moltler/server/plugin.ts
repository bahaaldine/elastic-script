import type {
  PluginInitializerContext,
  CoreSetup,
  CoreStart,
  Plugin,
  Logger,
} from '@kbn/core/server';

import type { MoltlerPluginSetup, MoltlerPluginStart } from './types';
import { defineRoutes } from './routes';

export class MoltlerPlugin implements Plugin<MoltlerPluginSetup, MoltlerPluginStart> {
  private readonly logger: Logger;

  constructor(initializerContext: PluginInitializerContext) {
    this.logger = initializerContext.logger.get();
  }

  public setup(core: CoreSetup) {
    this.logger.debug('moltler: Setup');
    const router = core.http.createRouter();

    // Register server side APIs
    defineRoutes(router, this.logger);

    return {};
  }

  public start(core: CoreStart) {
    this.logger.debug('moltler: Started');
    return {};
  }

  public stop() {}
}
