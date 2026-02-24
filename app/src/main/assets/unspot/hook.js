(() => {
  window.webpackChunkclient_web = window.webpackChunkclient_web || [];

  const definePlatform = () => {
    Object.defineProperty(window, "Platform", { value: Object.getOwnPropertyNames(Platform).reduce((p, c) => ({ ...p, [c]: Platform[c] }), {}) });
  };

  window.webpackChunkclient_web.push([
    ["__platform_hook__"],
    {},
    function installHook(require) {
      const originalD = require.d.bind(require);
      require.d = function (exports, descriptors) {
        if (Object.prototype.hasOwnProperty.call(descriptors, "createPlatformWeb")) {
          const originalGetter = descriptors.createPlatformWeb;
          descriptors = Object.assign({}, descriptors, {
            createPlatformWeb: function () {
              const originalFn = originalGetter();
              return async function createPlatformWeb(...args) {
                const platform = await originalFn.apply(this, args);
                window._Platform = platform;
                definePlatform();
                return platform;
              };
            },
          });
        }
        originalD(exports, descriptors);
      };
    },
  ]);

  const Platform = new Proxy(
    {},
    {
      get: function (_, prop) {
        if (!window._Platform) return undefined;
        if (prop === "then") return Promise.resolve(window._Platform);
        return window._Platform.getRegistry()._map.get(Symbol.for(prop)).instance;
      },
      ownKeys: function () {
        if (!window._Platform) return [];
        return [
          ...window._Platform
            .getRegistry()
            ._map.keys()
            .map((k) => Symbol.keyFor(k)),
        ];
      },
    },
  );
})();
