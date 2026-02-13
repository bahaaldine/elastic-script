import type {
  PluginInitializerContext,
  CoreSetup,
  CoreStart,
  Plugin,
  Logger,
} from '@kbn/core/server';

import { defineRoutes } from './routes';

export interface MoltlerServerPluginSetup {}
export interface MoltlerServerPluginStart {}

export class MoltlerServerPlugin
  implements Plugin<MoltlerServerPluginSetup, MoltlerServerPluginStart>
{
  private readonly logger: Logger;

  constructor(initializerContext: PluginInitializerContext) {
    this.logger = initializerContext.logger.get();
  }

  public setup(core: CoreSetup) {
    this.logger.debug('Moltler server plugin setup');

    const router = core.http.createRouter();

    // Register routes
    defineRoutes(router, this.logger);

    return {};
  }

  public start(core: CoreStart) {
    this.logger.debug('Moltler server plugin started');
    return {};
  }

  public stop() {}
}
