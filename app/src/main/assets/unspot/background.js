// https://github.com/Isaaker/Spotify-AdsList
const spotifyTelemetry = [
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
const adApiPaths = [
  /\/ads\//,
  /\/ad-logic\//,
  /\/gabo-receiver-service\//,
];

const adAudioPatterns = [
  /\/mp3\/(ad|preview)/i,
];

const isSpotify = /(^|\.)spotify\.com$/;

const patterns = [
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
  const url = details.url;
  const origin = details.originUrl || details.documentUrl || "";
  return url.includes("accounts.spotify.com") || origin.includes("accounts.spotify.com");
}

function isAdAudioRequest(url) {
  let i;
  for (i = 0; i < adAudioPatterns.length; i++) {
    if (adAudioPatterns[i].test(url)) return true;
  }
  return false;
}

browser.webRequest.onBeforeRequest.addListener(
  (details) => {
    if (isLoginRelated(details)) return;
    const url = details.url;
    const host = (url.split("/")[2] || "").split(":")[0];
    let path;
    let j;
    if (isAdAudioRequest(url)) {
      return { cancel: true };
    }
    if (details.type === "image") {
      return { cancel: true };
    }
    if (/\.(woff2?|ttf|otf|eot)(\?|$)/.test(url)) {
      return { cancel: true };
    }
    if (/spclient.*\.spotify\.com/.test(host)) {
      path = url.replace(/^https?:\/\/[^/]+/, "");
      for (j = 0; j < adApiPaths.length; j++) {
        if (adApiPaths[j].test(path)) {
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

const cachePatterns = [
  "*://*.spotify.com/*",
  "*://*.scdn.co/*",
  "*://*.spotifycdn.com/*",
];

browser.webRequest.onHeadersReceived.addListener(
  (details) => {
    if (isLoginRelated(details)) return;
    const url = details.url;
    const headers = details.responseHeaders || [];
    let i;
    let found;
    // allow MAIN world script
    if (/open\.spotify\.com/.test(url)) {
      for (i = 0; i < headers.length; i++) {
        if (headers[i].name.toLowerCase() === "content-security-policy") {
          headers[i].value = "";
        }
      }
    }
    const isStatic = /\.(js|css|woff2?|ttf|otf|eot|png|jpe?g|gif|webp|svg|ico|woff)(\?|$)/i.test(url) ||
      /\.scdn\.co$/.test((url.split("/")[2] || "").split(":")[0]) ||
      /\.spotifycdn\.com$/.test((url.split("/")[2] || "").split(":")[0]);
    if (!isStatic) return { responseHeaders: headers };
    found = false;
    for (i = 0; i < headers.length; i++) {
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
