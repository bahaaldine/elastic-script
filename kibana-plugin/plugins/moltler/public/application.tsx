import React from 'react';
import ReactDOM from 'react-dom';
import type { CoreStart, AppMountParameters } from '@kbn/core/public';

const MoltlerApp = () => {
  return (
    <div style={{ padding: '20px' }}>
      <h1>Moltler Skills Manager</h1>
      <p>Hello from Moltler!</p>
    </div>
  );
};

export const renderApp = (core: CoreStart, { element }: AppMountParameters) => {
  ReactDOM.render(<MoltlerApp />, element);
  return () => ReactDOM.unmountComponentAtNode(element);
};
