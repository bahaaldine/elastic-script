import type { PluginInitializerContext } from '@kbn/core/server';
import { MoltlerServerPlugin } from './plugin';

export function plugin(initializerContext: PluginInitializerContext) {
  return new MoltlerServerPlugin(initializerContext);
}

export type { MoltlerServerPluginSetup, MoltlerServerPluginStart } from './plugin';
