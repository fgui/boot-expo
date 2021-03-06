import Expo from 'expo';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';


// Runtime string -> module map for `(js/require ...)`

const moduleMap = {
  'expo': require('expo'),
  'react': require('react'),
  'react-native': require('react-native'),
};
const oldRequire = require;
global.require = (m) => moduleMap[m] || oldRequire(m);


// Remote JavaScript loader for `goog.require(...)` for development

const packagerHost = Expo.Constants.manifest.bundleUrl
                         .match(/^https?:\/\/.*?\//)[0];

let lastLoadTargetAsync = Promise.resolve();
const loadTargetAsync = (path, evaluator) => (
  lastLoadTargetAsync = Promise.all([
    fetch(`${packagerHost}target/main.out/${path}`)
      .then((response) => response.text()),
    lastLoadTargetAsync,
  ]).then(([text]) => {
    (evaluator || eval)(text);

    // Shim `goog.net.jsloader` to use our loader
    if (path === 'goog/net/jsloader.js') {
      goog.net.jsloader.safeLoad = (path) => {
        const deferred = new goog.async.Deferred();
        (async () => {
          try {
            const unwrapped = goog.html.TrustedResourceUrl.unwrap(path)
                                  .path_;
            // These paths happen to be relative to `target/`
            await loadTargetAsync(`../${unwrapped}`);
            deferred.callback();
          } catch (e) {
            deferred.errback();
          }
        })();
        return deferred;
      }
    }
  })
);


// Load and enter CLJS entrypoint for development

const initCLJSDevAsync = async () => {
  // Load and configure Google Closure Library base
  await loadTargetAsync('goog/base.js');
  goog.basePath = 'goog/';
  goog.global.CLOSURE_IMPORT_SCRIPT = (src) =>
    (loadTargetAsync(src), true);

  // The `boot-cljs` output entrypoint (`target/main.js`) relies on the DOM to
  // load the "Boot CLJS Main Namespace".
  // See: https://github.com/boot-clj/boot-cljs/blob/master/docs/compiler-options.md#main
  // This namespace looks like `boot.cljs.main1234`, with the number generated
  // using `gensym`. We extract this name, load its dependencies manually and then
  // load it directly with `goog.require(...)`, which we can use now since we
  // loaded `goog/base.js` above.
  await loadTargetAsync('cljs_deps.js');
  await loadTargetAsync('goog/deps.js');
  await loadTargetAsync('../main.js', (text) =>
    goog.require(text.match(/boot.cljs.main\d+/)[0]));
}


// Displays component set by `(js/setCLJSRootElement ...)`

class App extends React.Component {
  state = {
    cljsRoot: null,
  }

  componentDidMount() {
    global.setCLJSRootElement = (cljsRoot) =>
      this.setState({ cljsRoot });
    initCLJSDevAsync();
  }

  render() {
    return this.state.cljsRoot || (
      <View style={{
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
      }}>
        <Text>waiting for cljs root element...</Text>
      </View>
    );
  }
}

Expo.registerRootComponent(App);
