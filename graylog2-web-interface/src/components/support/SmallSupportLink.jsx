import React from 'react';

const SmallSupportLink = React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
  },
  render() {
    return (
      <p className="description-tooltips description-tooltips-small">
        <strong>
          {this.props.children}
        </strong>
      </p>
    );
  },
});

export default SmallSupportLink;
