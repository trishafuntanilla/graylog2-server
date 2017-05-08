import React from 'react';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import { AlertNotificationsComponent } from 'components/alertnotifications';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const AlertNotificationsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DocumentTitle title="Alert notifications">
        <div>
          <PageHeader title="Manage alert notifications">
            <span>
              Notifications let you be aware of changes in your alert conditions status any time. Notifications can be sent directly to you or to other systems you use for that purpose.
            <br/><br/>
              <strong>Remember to assign the notifications to use in the alert conditions page.</strong>
            </span>

            <span></span>

            <span>
              <LinkContainer to={Routes.ALERTS.CONDITIONS}>
                <Button bsStyle="info">Manage conditions</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <AlertNotificationsComponent />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default AlertNotificationsPage;
