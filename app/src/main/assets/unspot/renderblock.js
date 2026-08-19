(() => {
  try {
    navigator.serviceWorker.getRegistrations().then((regs) => {
      regs.forEach((r) => {
        r.unregister();
      });
    });
  } catch (_e) {}

  Object.defineProperty(navigator, "serviceWorker", {
    configurable: true,
    writable: false,
    value: {
      register: () => Promise.reject(new Error("SW disabled")),
      getRegistrations: () => Promise.resolve([]),
      ready: new Promise(() => {}),
      controller: null,
      addEventListener: () => {},
      removeEventListener: () => {},
    },
  });

  var isLoginPage =
    window.location.href.indexOf("accounts.spotify.com") !== -1 ||
    window.location.href.indexOf("/login") !== -1 ||
    window.location.href.indexOf("accounts.scdn.co") !== -1;
  var s = null;

  if (!isLoginPage) {
    s = document.createElement("style");
    s.textContent =
      "html{display:none!important}*,*::before,*::after{animation:none!important;transition:none!important;animation-duration:0s!important;transition-duration:0s!important;caret-color:transparent!important}";
    document.documentElement.appendChild(s);

    window.IntersectionObserver = () => ({
      observe: () => {},
      unobserve: () => {},
      disconnect: () => {},
      takeRecords: () => [],
      root: null,
      rootMargin: "",
      thresholds: [],
    });
    window.ResizeObserver = () => ({
      observe: () => {},
      unobserve: () => {},
      disconnect: () => {},
    });
    HTMLCanvasElement.prototype.getContext = () => null;
  }
})();
