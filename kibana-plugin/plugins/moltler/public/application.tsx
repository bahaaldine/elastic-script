import React from 'react';
import ReactDOM from 'react-dom';
import type { CoreStart, AppMountParameters } from '@kbn/core/public';
import { MoltlerApp } from './components/MoltlerApp';

export const renderApp = (core: CoreStart, { element }: AppMountParameters) => {
  ReactDOM.render(
    <MoltlerApp http={core.http} notifications={core.notifications} />,
    element
  );

  return () => ReactDOM.unmountComponentAtNode(element);
};
