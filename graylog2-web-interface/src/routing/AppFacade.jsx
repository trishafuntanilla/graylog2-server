import React from 'react';
import Reflux from 'reflux';
import LoginPage from 'react-proxy?name=LoginPage!pages/LoginPage';
import LoadingPage from 'react-proxy?name=LoadingPage!pages/LoadingPage';
import LoggedInPage from 'react-proxy?name=LoggedInPage!pages/LoggedInPage';
import ServerUnavailablePage from 'pages/ServerUnavailablePage';

import StoreProvider from 'injection/StoreProvider';
const SessionStore = StoreProvider.getStore('Session');
import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');
const ServerAvailabilityStore = StoreProvider.getStore('ServerAvailability');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import 'bootstrap/less/bootstrap.less';
import 'font-awesome/css/font-awesome.css';
import 'opensans-npm-webfont';
import 'stylesheets/bootstrap-submenus.less';
import 'toastr/toastr.less';
import 'rickshaw/rickshaw.css';
import 'stylesheets/graylog2.less';

const AppFacade = React.createClass({
  mixins: [Reflux.connect(SessionStore), Reflux.connect(ServerAvailabilityStore), Reflux.connect(CurrentUserStore)],

  componentDidMount() {
    this.interval = setInterval(ServerAvailabilityStore.ping, 20000);
    SessionActions.login("admin", "admin", document.location.host);
    SessionActions.validate();
  },

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },

  render() {
    if (!this.state.server.up) {
      return <ServerUnavailablePage server={this.state.server} />;
    }
    if (!this.state.sessionId) {
      return <LoadingPage text="Loading..."/>;
    }
    if (!this.state.currentUser) {
      return <LoadingPage text="Loading..."/>;
    }
    return <LoggedInPage />;
  },
});

export default AppFacade;
