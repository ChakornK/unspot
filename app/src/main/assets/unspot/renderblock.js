(function () {
  var s = document.createElement("style");
  s.textContent = "html{display:none!important}*,*::before,*::after{animation:none!important;transition:none!important;animation-duration:0s!important;transition-duration:0s!important;caret-color:transparent!important}";
  document.documentElement.appendChild(s);

  var rid = 1;
  window.requestAnimationFrame = function () { return rid++; };
  window.cancelAnimationFrame = function () {};
})();