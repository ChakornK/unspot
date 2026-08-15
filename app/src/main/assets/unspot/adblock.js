// unspot adblock: strips ad tracks from the Spotify web player state machine and
// mutes any ad media element that still gets created. Ads ride the same audio-fa
// + widevine pipeline as tracks, so the DOM layer must silence them. Muting (not
// removing/erroring) the element lets the ESK see a successful playback and
// advance to the next track.
(function () {
  "use strict";

  var authorization = "";
  var deviceId = "";
  var originalFetch = window.fetch;

  // Ads play through the same hosts as real tracks; the only reliable element-level
  // signals are a direct (non-blob) ad CDN URL or a short duration (ads: 15/30/60s).
  var isAdMedia = function (el) {
    var src = String(el.currentSrc || el.src || "");
    if (/:ad:/.test(src)) return true;
    if (src.includes("adstudio") || src.includes("audio-ads")) return true;
    var dur = el.duration || 0;
    if (dur > 0 && dur < 67) return true;
    return false;
  };

  // Force volume/muted to 0 (with hardened property descriptors) so the ad plays
  // silently to its natural end and the ESK advances. Avoids erroring the element.
  var muteEl = function (el) {
    try { el.muted = true; } catch (e) {}
    try { el.volume = 0; } catch (e) {}
    try {
      Object.defineProperty(el, "volume", { configurable: true, get: function () { return 0; }, set: function () {} });
      Object.defineProperty(el, "muted", { configurable: true, get: function () { return true; }, set: function () {} });
    } catch (e) {}
    if (el.setAttribute) { try { el.setAttribute("muted", ""); } catch (e) {} }
  };

  // Mute + jump to end + fire ended so a detected ad completes instantly.
  var killAd = function (el) {
    var oldSrc = String(el.currentSrc || el.src || "");
    var dur = el.duration || 0;
    muteEl(el);
    try { el.currentTime = dur; } catch (e) {}
    try { el.pause(); } catch (e) {}
    try { el.dispatchEvent(new Event("ended", { bubbles: false })); } catch (e) {}
    var guard = setInterval(function () {
      muteEl(el);
      var cur = String(el.currentSrc || el.src || "");
      if ((el.duration || 0) >= 67 && cur !== oldSrc) { clearInterval(guard); return; }
      try { el.currentTime = el.duration || 999999; } catch (e) {}
      try { el.pause(); } catch (e) {}
    }, 200);
    setTimeout(function () { try { clearInterval(guard); } catch (e) {} }, 4000);
  };

  // Hook createElement: wire guards onto every media element as it's made.
  try {
    var origCreate = document.createElement;
    document.createElement = function (tag) {
      var el = origCreate.apply(this, arguments);
      if (el instanceof HTMLMediaElement) {
        el.addEventListener("play", function () {
          if (isAdMedia(this)) killAd(this);
        }, true);
        el.addEventListener("loadedmetadata", function () {
          if (isAdMedia(this)) { try { this.pause(); } catch (e) {} }
        }, true);
        el.addEventListener("timeupdate", function () {
          if ((this.duration || 0) > 0 && (this.duration || 0) < 67 && !this.paused) killAd(this);
        }, true);
        var oldPlay = el.play;
        el.play = function () {
          if (isAdMedia(this)) { killAd(this); return Promise.resolve(); }
          return oldPlay.apply(this, arguments);
        };
        // setAttribute("src", adUrl) bypasses the src property setter
        var oldSetAttr = el.setAttribute;
        el.setAttribute = function (name, value) {
          if (name === "src") {
            var s = String(value || "");
            if (/adstudio/i.test(s) || /audio-ads/i.test(s) || /audio-fa\.scdn\.co/i.test(s)) {
              muteEl(this);
            }
          }
          return oldSetAttr.apply(this, arguments);
        };
      }
      return el;
    };
  } catch (e) {}

  // Prototype-level guards for elements created before our hooks (incl. new Audio()).
  try {
    var origProtoPlay = HTMLMediaElement.prototype.play;
    HTMLMediaElement.prototype.play = function () {
      if (isAdMedia(this)) { killAd(this); return Promise.resolve(); }
      return origProtoPlay.apply(this, arguments);
    };
    document.addEventListener("play", function (e) {
      var t = e.target;
      if (t && t.duration && t.duration < 67 && t.duration > 0) killAd(t);
    }, true);
    document.addEventListener("loadedmetadata", function (e) {
      var t = e.target;
      if (t && t.duration && t.duration < 67 && t.duration > 0) killAd(t);
    }, true);
    document.addEventListener("timeupdate", function (e) {
      var t = e.target;
      if (t && t instanceof HTMLMediaElement && !t.paused && isAdMedia(t)) killAd(t);
    }, true);
  } catch (e) {}

  // Prototype-level src guard: ads are assigned a DIRECT scdn URL (tracks use
  // blob:/MSE). For ads, let the element play its real source but MUTED so the
  // ESK sees a successful playback and advances (blob-substitution errors + sticks).
  try {
    var origSrcDesc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, "src");
    Object.defineProperty(HTMLMediaElement.prototype, "src", {
      configurable: true,
      get: function () { return origSrcDesc.get.call(this); },
      set: function (v) {
        var s = String(v || "");
        var isAdLike = /audio-fa\.scdn\.co\/audio/i.test(s) || /adstudio/i.test(s) || /audio-ads/i.test(s) || /\.scdn\.co\/mp3\//i.test(s) || /^spotify-audio:\/\//i.test(s);
        if (isAdLike && !s.startsWith("blob:")) {
          var self = this;
          muteEl(self);
          var guard = setInterval(function () { muteEl(self); }, 300);
          var stopGuard = function () {
            clearInterval(guard);
            self.removeEventListener("ended", stopGuard);
            self.removeEventListener("error", stopGuard);
          };
          self.addEventListener("ended", stopGuard);
          self.addEventListener("error", stopGuard);
          return origSrcDesc.set.call(this, v);
        }
        return origSrcDesc.set.call(this, v);
      }
    });
  } catch (e) {}

  // Periodic scanner: catch media elements that loaded an ad even if created before
  // our hooks (pierces shadow roots). Duration check covers blob-src ads (tracks are
  // 120s+; ads are 15/30/60s).
  setInterval(function () {
    try {
      var scanEls;
      function collectSc(root) {
        var w = root.querySelectorAll ? root.querySelectorAll("audio,video") : [];
        for (var i = 0; i < w.length; i++) scanEls.push(w[i]);
        var hosts = root.querySelectorAll ? root.querySelectorAll("*") : [];
        for (var j = 0; j < hosts.length; j++) {
          if (hosts[j].shadowRoot) collectSc(hosts[j].shadowRoot);
        }
      }
      scanEls = [];
      collectSc(document);
      for (var i = 0; i < scanEls.length; i++) {
        var el = scanEls[i];
        if (isAdMedia(el) && !el.paused) killAd(el);
      }
    } catch (e) {}
  }, 200);


  var _WS = WebSocket;

  function processWsMessage(event) {
    try {
      var data = JSON.parse(event.data);
      if (!data.payloads) return event;

      var modified = false;
      for (var i = 0; i < data.payloads.length; i++) {
        var payload = data.payloads[i];

        // Drop ad payload
        if (payload.cluster && payload.cluster.player_state && payload.cluster.player_state.track) {
          var ctrack = payload.cluster.player_state.track;
          var adCluster = /:ad:/.test(String(ctrack.uri || "")) ||
                          /^ads\//.test(String(ctrack.provider || "")) ||
                          /^ads\//.test(String(ctrack.uri || "")) ||
                          String(ctrack.content_type || "").toUpperCase() === "AD";
          if (adCluster) {
            data.payloads.splice(i, 1);
            i--;
            modified = true;
            continue;
          }
        }

        if (payload.type === "replace_state" && payload.state_machine) {
          var sm = payload.state_machine;
          if (sm && sm.states && sm.tracks) {
            for (var j = 0; j < sm.states.length; j++) {
              if (isAdState(sm.states[j], sm)) {
                var next = getNextNonAdState(sm, j);
                if (next) {
                  var rep = JSON.parse(JSON.stringify(next));
                  rep.state_id = sm.states[j].state_id;
                  sm.states[j] = rep;
                } else {
                  sm.states[j] = shortenState(sm.states[j], sm.tracks[sm.states[j].track]);
                }
                modified = true;
              }
            }
            payload.state_machine = sm;
            data.payloads[i] = payload;
          }
        }
      }

      if (modified) {
        return new MessageEvent(event.type, {
          data: JSON.stringify(data),
          origin: event.origin,
          lastEventId: event.lastEventId,
          source: event.source,
          ports: event.ports
        });
      }
      return event;
    } catch (e) {
      return event;
    }
  }

  WebSocket = function (url, protocols) {
    var ws = protocols ? new _WS(url, protocols) : new _WS(url);
    var _origOnMessage = null;

    ws.addEventListener("message", function (event) {
      if (!_origOnMessage) return;
      var processed = processWsMessage(event);
      _origOnMessage.call(ws, processed);
    });

    Object.defineProperty(ws, "onmessage", {
      get: function () { return _origOnMessage; },
      set: function (fn) { _origOnMessage = fn; }
    });

    var _origAddEventListener = ws.addEventListener.bind(ws);
    ws.addEventListener = function (type, listener, options) {
      if (type === "message" && typeof listener === "function") {
        var wrappedListener = function (event) {
          var processed = processWsMessage(event);
          listener.call(ws, processed);
        };
        _origAddEventListener(type, wrappedListener, options);
      } else {
        _origAddEventListener(type, listener, options);
      }
    };

    return ws;
  };
  WebSocket.prototype = _WS.prototype;
  WebSocket.CONNECTING = _WS.CONNECTING;
  WebSocket.OPEN = _WS.OPEN;
  WebSocket.CLOSING = _WS.CLOSING;
  WebSocket.CLOSED = _WS.CLOSED;

  function isAdTrack(track) {
    if (!track || !track.metadata) return false;
    var uri = track.metadata.uri || track.uri || "";
    return uri.includes(":ad:") || track.content_type === "AD";
  }

  function isAdState(state, stateMachine) {
    if (!state) return false;
    var track = stateMachine.tracks[state.track];
    return isAdTrack(track);
  }

  function getNextNonAdState(stateMachine, fromIndex) {
    var states = stateMachine.states;
    var visited = {};
    var idx = fromIndex;
    var maxIter = states.length + 1;
    while (maxIter-- > 0) {
      var state = states[idx];
      if (!state) return null;
      var advance = state.transitions && state.transitions.advance;
      if (!advance) return null;
      var nextIdx = advance.state_index;
      if (visited[nextIdx]) return null;
      visited[nextIdx] = true;
      var nextState = states[nextIdx];
      if (!nextState) return null;
      if (!isAdState(nextState, stateMachine)) return nextState;
      idx = nextIdx;
    }
    return null;
  }

  function shortenState(state, track) {
    var duration = (track && track.metadata && track.metadata.duration) || 0;
    state.disallow_seeking = false;
    state.restrictions = {};
    state.initial_playback_position = duration;
    state.position_offset = duration;
    return state;
  }

  async function fetchMoreStates(stateMachineId, stateId) {
    if (!authorization || !deviceId) return null;
    var url = "https://spclient.wg.spotify.com/track-playback/v1/devices/" + deviceId + "/state";
    var body = JSON.stringify({
      seq_num: Date.now(),
      state_ref: { state_machine_id: stateMachineId, state_id: stateId, paused: false },
      sub_state: { playback_speed: 1, position: 0, duration: 0, stream_time: 0, media_type: "AUDIO", bitrate: 160000 },
      previous_position: 0,
      debug_source: "resume"
    });
    try {
      var resp = await originalFetch.call(window, url, {
        method: "PUT",
        headers: { "Authorization": authorization, "Content-Type": "application/json" },
        body: body
      });
      if (resp.status !== 200) return null;
      var data = await resp.json();
      return data.state_machine || null;
    } catch (e) { return null; }
  }

  async function manipulateStateMachine(stateMachine) {
    if (!stateMachine || !stateMachine.states || !stateMachine.tracks) return stateMachine;

    var states = stateMachine.states;
    var tracks = stateMachine.tracks;

    for (var i = 0; i < states.length; i++) {
      var state = states[i];
      if (!isAdState(state, stateMachine)) continue;

      var track = tracks[state.track];
      var nextState = getNextNonAdState(stateMachine, i);

      if (nextState) {
        var replacement = JSON.parse(JSON.stringify(nextState));
        replacement.state_id = state.state_id;
        states[i] = replacement;
      } else {
        var fetched = await fetchMoreStates(
          stateMachine.state_machine_id,
          state.state_id
        );
        if (fetched) {
          var fetchedNext = null;
          for (var j = 0; j < (fetched.states || []).length; j++) {
            var fs = fetched.states[j];
            var ft = fetched.tracks[fs.track];
            if (!isAdTrack(ft)) { fetchedNext = fs; break; }
          }
          if (fetchedNext) {
            var newTrackIdx = tracks.length;
            tracks.push(fetched.tracks[fetchedNext.track]);
            fetchedNext.track = newTrackIdx;
            fetchedNext.state_id = state.state_id;
            fetchedNext.transitions = {};
            states[i] = fetchedNext;
          } else {
            states[i] = shortenState(state, track);
          }
        } else {
          states[i] = shortenState(state, track);
        }
      }
    }

    stateMachine.states = states;
    stateMachine.tracks = tracks;
    return stateMachine;
  }

  window.fetch = function (url, init) {
    var urlStr = typeof url === "string" ? url : (url && url.url) || "";
    var method = (init && init.method) || (url && url.method) || "GET";

    if (urlStr.includes("spclient.wg.spotify.com")) {
      var h = null;
      if (init && init.headers) {
        h = init.headers;
      } else if (url && typeof url !== "string" && url.headers) {
        h = url.headers;
      }
      if (h) {
        var auth = h.authorization || h.Authorization || (h.get && h.get("authorization"));
        if (auth) authorization = auth;
      }
    }

    if (urlStr.endsWith("/devices") && init && init.body) {
      try {
        var parsed = JSON.parse(init.body);
        if (parsed.device && parsed.device.device_id) {
          deviceId = parsed.device.device_id;
        }
      } catch (e) {}
    }

    if (method.toUpperCase() === "PUT" && urlStr.endsWith("/state") && urlStr.includes("spclient.wg.spotify.com")) {
      return originalFetch.call(window, url, init).then(function (response) {
        var clone = response.clone();
        return response.json().then(async function (data) {
          if (!data || !data.state_machine) return clone;
          try {
            data.state_machine = await manipulateStateMachine(data.state_machine);
            return new Response(JSON.stringify(data), {
              status: response.status,
              statusText: response.statusText,
              headers: response.headers
            });
          } catch (e) {
            console.error("[unspot] fetch manipulation failed:", e);
            return clone;
          }
        }).catch(function () { return clone; });
      });
    }

    return originalFetch.call(window, url, init);
  };
})();
