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

// Ad API path patterns to block (uBlock/abba23 style denylist)
var adApiPaths = [
  /\/ads\//,
  /\/ad-logic\//,
  /\/gabo-receiver-service\//,
];

// Ad audio hosts/paths to block at the network layer (catches iframes/workers/MediaSource)
// NOTE: we deliberately DO NOT block ad audio downloads — the DOM layer mutes the ad
// element (see adblock.js muteEl) so the ESK sees a successful playback and advances
// to the next track. Blocking the bytes errors the element and sticks the player.
var adAudioPatterns = [
  /\/mp3\/(ad|preview)/i,
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

function isAdAudioRequest(url) {
  for (var i = 0; i < adAudioPatterns.length; i++) {
    if (adAudioPatterns[i].test(url)) return true;
  }
  return false;
}

browser.webRequest.onBeforeRequest.addListener(
  function (details) {
    if (isLoginRelated(details)) return;
    var url = details.url;
    var host = (url.split("/")[2] || "").split(":")[0];
    if (isAdAudioRequest(url)) {
      return { cancel: true };
    }
    if (details.type === "image") {
      return { cancel: true };
    }
    if (/\.(woff2?|ttf|otf|eot)(\?|$)/.test(url)) {
      return { cancel: true };
    }
    // Block ad API endpoints (uBlock-style denylist)
    if (/spclient.*\.spotify\.com/.test(host)) {
      var path = url.replace(/^https?:\/\/[^/]+/, "");
      for (var i = 0; i < adApiPaths.length; i++) {
        if (adApiPaths[i].test(path)) {
          return { cancel: true };
        }
      }
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
    var headers = details.responseHeaders || [];
    // Relax CSP on Spotify pages to allow our MAIN world script
    if (/open\.spotify\.com/.test(url)) {
      for (var i = 0; i < headers.length; i++) {
        if (headers[i].name.toLowerCase() === "content-security-policy") {
          headers[i].value = "";
        }
      }
    }
    var isStatic = /\.(js|css|woff2?|ttf|otf|eot|png|jpe?g|gif|webp|svg|ico|woff)(\?|$)/i.test(url) ||
      /\.scdn\.co$/.test((url.split("/")[2] || "").split(":")[0]) ||
      /\.spotifycdn\.com$/.test((url.split("/")[2] || "").split(":")[0]);
    if (!isStatic) return { responseHeaders: headers };
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
