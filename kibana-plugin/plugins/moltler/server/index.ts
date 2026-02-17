import type { PluginInitializerContext } from '@kbn/core/server';
import { MoltlerPlugin } from './plugin';

export function plugin(initializerContext: PluginInitializerContext) {
  return new MoltlerPlugin(initializerContext);
}

export type { MoltlerPluginSetup, MoltlerPluginStart } from './types';
