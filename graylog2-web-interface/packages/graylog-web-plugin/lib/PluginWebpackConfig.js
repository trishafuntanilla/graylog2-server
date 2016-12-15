'use strict';

var webpack = require('webpack');
var merge = require('webpack-merge');
var path = require('path');
var HtmlWebpackPlugin = require('html-webpack-plugin');

var defaultRootPath = path.resolve(module.parent.parent.filename, '../');
var defaultOptions = {
  root_path: defaultRootPath,
  entry_path: path.resolve(defaultRootPath, 'src/web/index.jsx'),
  build_path: path.resolve(defaultRootPath, 'build')
};

function getPluginFullName(fqcn) {
  return 'plugin.' + fqcn;
}

function PluginWebpackConfig(fqcn, _options, additionalConfig) {
  var options = merge(defaultOptions, _options);
  var VENDOR_MANIFEST = require(path.resolve(_options.web_src_path, 'manifests', 'vendor-manifest.json'));

  var plugins = [new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: options.root_path }), new HtmlWebpackPlugin({ filename: getPluginFullName(fqcn) + '.module.json', inject: false, template: path.resolve(_options.web_src_path, 'templates', 'module.json.template') })];

  var config = merge.smart(require(path.resolve(_options.web_src_path, 'webpack.config.js')), {
    output: {
      path: options.build_path
    },
    plugins: plugins,
    resolve: {
      modulesDirectories: [path.resolve(options.entry_path, '..')]
    }
  });

  var entry = {};
  entry[getPluginFullName(fqcn)] = options.entry_path;

  config.entry = entry;

  if (additionalConfig) {
    return merge.smart(config, additionalConfig);
  }

  return config;
}

module.exports = PluginWebpackConfig;