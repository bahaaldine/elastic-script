import { MoltlerPlugin } from './plugin';

export function plugin() {
  return new MoltlerPlugin();
}

export type { MoltlerPluginSetup, MoltlerPluginStart } from './plugin';
