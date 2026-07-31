browser.webRequest.onBeforeRequest.addListener(
  function (details) {
    var url = details.url;
    if (
      /scdn\.co/.test(url) ||
      /spotifycdn\.com/.test(url) ||
      /\.(woff2?|ttf|otf|eot)(\?|$)/.test(url)
    ) {
      return { cancel: true };
    }
  },
  { urls: ["*://*.spotify.com/*"] },
  ["blocking"]
);
