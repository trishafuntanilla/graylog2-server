import React from 'react';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import { DocumentTitle, PageHeader } from 'components/common';
import OutputsComponent from 'components/outputs/OutputsComponent';

const SystemOutputsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DocumentTitle title="Outputs">
        <span>
          <PageHeader title="Outputs in Cluster">
            <span>
              Nodes can forward messages via outputs. Launch or terminate as many outputs as you want here{' '}
              <strong>and then assign them to streams to forward all messages of a stream in real-time.</strong>
            </span>
          </PageHeader>

          <OutputsComponent permissions={this.state.currentUser.permissions}/>
        </span>
      </DocumentTitle>
    );
  },
});

export default SystemOutputsPage;
