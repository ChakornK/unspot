(function () {
  try {
    navigator.serviceWorker.getRegistrations().then(function (regs) {
      regs.forEach(function (r) { r.unregister(); });
    });
  } catch (e) {}

  Object.defineProperty(navigator, "serviceWorker", {
    configurable: true,
    writable: false,
    value: {
      register: function () { return Promise.reject(new Error("SW disabled")); },
      getRegistrations: function () { return Promise.resolve([]); },
      ready: new Promise(function () {}),
      controller: null,
      addEventListener: function () {},
      removeEventListener: function () {}
    }
  });

  var isLoginPage = window.location.href.indexOf("accounts.spotify.com") !== -1 ||
    window.location.href.indexOf("/login") !== -1 ||
    window.location.href.indexOf("accounts.scdn.co") !== -1;

  if (!isLoginPage) {
    var s = document.createElement("style");
    s.textContent = "html{display:none!important}*,*::before,*::after{animation:none!important;transition:none!important;animation-duration:0s!important;transition-duration:0s!important;caret-color:transparent!important}";
    document.documentElement.appendChild(s);

    var rid = 1;
    window.requestAnimationFrame = function () { return rid++; };
    window.cancelAnimationFrame = function () {};

    window.IntersectionObserver = function () {
      return { observe: function () {}, unobserve: function () {}, disconnect: function () {}, takeRecords: function () { return []; }, root: null, rootMargin: "", thresholds: [] };
    };

    window.ResizeObserver = function () {
      return { observe: function () {}, unobserve: function () {}, disconnect: function () {} };
    };

    HTMLCanvasElement.prototype.getContext = function () { return null; };
  }

  (function () {
    var origFetch = window.fetch;
    var origXHROpen = XMLHttpRequest.prototype.open;
    var count = 0;
    window.fetch = function () {
      count++;
      var url = arguments[0];
      if (typeof url === "string") console.log("NET", count, "fetch", url.substring(0, 150));
      return origFetch.apply(this, arguments);
    };
    XMLHttpRequest.prototype.open = function () {
      count++;
      console.log("NET", count, "xhr", arguments[1] ? arguments[1].substring(0, 150) : "");
      return origXHROpen.apply(this, arguments);
    };
  })();
})();