// https://github.com/Isaaker/Spotify-AdsList
var spotifyTelemetry = [
  "log.spotify.com",
  "log2.spotify.com",
  "analytics.spotify.com",
  "analytics.spotify.net",
  "pixel.spotify.com",
  "pixel-static.spotify.com",
  "metrics.spotify.com",
  "adeventtracker.spotify.com",
  "bloodhound.spotify.com",
  "bloodhound-off-helios.spotify.com",
  "crashdump.spotify.com",
  "metadatafeedback.spotify.com",
  "artistinsights-realtime.spotify.com",
  "insights.spotify.com",
  "o11.em.spotify.com",
  "o1.em.spotify.com",
  "audio-ads.spotify.com",
  "ads.spotify.com",
  "adlab.spotify.com",
];

var isSpotify = /(^|\.)spotify\.com$/;

var patterns = [
  "*://*.spotify.com/*",
  "*://*.scdn.co/*",
  "*://*.doubleclick.net/*",
  "*://*.googlesyndication.com/*",
  "*://*.googleadservices.com/*",
  "*://*.googletagservices.com/*",
  "*://*.google-analytics.com/*",
  "*://*.moatads.com/*",
  "*://*.pubmatic.com/*",
  "*://*.adnxs.com/*",
  "*://*.scorecardresearch.com/*",
  "*://*.comscore.com/*",
  "*://*.mixpanel.com/*",
  "*://*.intercom.io/*",
  "*://*.adjust.com/*",
  "*://*.crashlytics.com/*",
  "*://*.sentry.io/*",
  "*://*.litix.io/*",
  "*://*.flurry.com/*",
  "*://*.adsafeprotected.com/*",
];

function isLoginRelated(details) {
  var url = details.url;
  var origin = details.originUrl || details.documentUrl || "";
  return url.includes("accounts.spotify.com") || origin.includes("accounts.spotify.com");
}

browser.webRequest.onBeforeRequest.addListener(
  function (details) {
    if (isLoginRelated(details)) return;
    var url = details.url;
    var host = (url.split("/")[2] || "").split(":")[0];
    if (details.type === "image") {
      return { cancel: true };
    }
    if (/\.(woff2?|ttf|otf|eot)(\?|$)/.test(url)) {
      return { cancel: true };
    }
    if (isSpotify.test(host)) {
      if (spotifyTelemetry.includes(host)) {
        return { cancel: true };
      }
    } else {
      return { cancel: false };
    }
  },
  { urls: patterns },
  ["blocking"],
);

var cachePatterns = [
  "*://*.spotify.com/*",
  "*://*.scdn.co/*",
  "*://*.spotifycdn.com/*",
];

browser.webRequest.onHeadersReceived.addListener(
  function (details) {
    if (isLoginRelated(details)) return;
    var url = details.url;
    var isStatic = /\.(js|css|woff2?|ttf|otf|eot|png|jpe?g|gif|webp|svg|ico|woff)(\?|$)/i.test(url) ||
      /\.scdn\.co$/.test((url.split("/")[2] || "").split(":")[0]) ||
      /\.spotifycdn\.com$/.test((url.split("/")[2] || "").split(":")[0]);
    if (!isStatic) return;
    var headers = details.responseHeaders || [];
    var found = false;
    for (var i = 0; i < headers.length; i++) {
      if (headers[i].name.toLowerCase() === "cache-control") {
        headers[i].value = "public, max-age=31536000, immutable";
        found = true;
        break;
      }
    }
    if (!found) {
      headers.push({ name: "Cache-Control", value: "public, max-age=31536000, immutable" });
    }
    return { responseHeaders: headers };
  },
  { urls: cachePatterns },
  ["blocking", "responseHeaders"],
);
