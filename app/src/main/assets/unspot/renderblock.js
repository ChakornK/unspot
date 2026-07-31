(function () {
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
})();