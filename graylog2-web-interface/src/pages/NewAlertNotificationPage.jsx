import React from 'react';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import { CreateAlertNotificationInput } from 'components/alertnotifications';

import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const NewAlertNotificationPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DocumentTitle title="New alert notification">
        <div>
          <PageHeader title="New alert notification">
            <span>
              Create a new notification that you can use to not miss any of your alerts.
            <br/><br/>
            <strong>Remember to assign the notifications to use in the alert conditions page.</strong>
            </span>

            <span></span>

            <span>
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS}>
                <Button bsStyle="info">Manage notifications</Button>
              </LinkContainer>
              &nbsp;
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <CreateAlertNotificationInput />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default NewAlertNotificationPage;
