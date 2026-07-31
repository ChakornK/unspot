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

var isSpotify = /(^|\.)spotify\.(com|net)$/;

var patterns = [
  "*://*.spotify.com/*",
  "*://*.spotify.net/*",
  "*://*.scdn.co/*",
  "*://*.spotifycdn.com/*",
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

browser.webRequest.onBeforeRequest.addListener(
  function (details) {
    var url = details.url;
    var host = (url.split("/")[2] || "").split(":")[0];
    if (/\.(woff2?|ttf|otf|eot)(\?|$)/.test(url)) {
      console.log("REQ BLOCK font", host, url.substring(0, 120));
      return { cancel: true };
    }
    if (/\.(png|jpe?g|gif|webp|svg|ico)(\?|$)/i.test(url) && (/\.scdn\.co$/.test(host) || /\.spotifycdn\.com$/.test(host))) {
      console.log("REQ BLOCK cdn-image", host, url.substring(0, 120));
      return { cancel: true };
    }
    if (isSpotify.test(host)) {
      if (spotifyTelemetry.indexOf(host) !== -1) {
        console.log("REQ BLOCK telemetry", host, url.substring(0, 120));
        return { cancel: true };
      }
      console.log("REQ ALLOW spotify", host, url.substring(0, 120));
    } else {
      console.log("REQ BLOCK 3rd-party", host, url.substring(0, 120));
      return { cancel: true };
    }
  },
  { urls: patterns },
  ["blocking"],
);
