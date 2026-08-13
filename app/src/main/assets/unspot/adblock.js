// unspot adblock: strips ad tracks from the Spotify web player state machine.
(function () {
  "use strict";

  var authorization = "";
  var deviceId = "";
  var totalAdsRemoved = 0;
  var originalFetch = window.fetch;
  var wsAccessToken = "";

  // --- WebSocket Hook ---

  var _WS = WebSocket;

  function processWsMessage(event) {
    try {
      var data = JSON.parse(event.data);
      if (!data.payloads) return event;

      var modified = false;
      for (var i = 0; i < data.payloads.length; i++) {
        var payload = data.payloads[i];

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
                  modified = true;
                  totalAdsRemoved++;
                  console.log("[unspot] WS removed ad state at index " + j);
                } else {
                  sm.states[j] = shortenState(sm.states[j], sm.tracks[sm.states[j].track]);
                  modified = true;
                  console.log("[unspot] WS shortened ad state at index " + j);
                }
              }
            }
            payload.state_machine = sm;
            data.payloads[i] = payload;
          }
        }

        if (payload.cluster && payload.cluster.player_state &&
            payload.cluster.player_state.track &&
            payload.cluster.player_state.track.provider === "ads/inject_tracks") {
          console.log("[unspot] blocked injected ad");
          payload.cluster.player_state.track = null;
          data.payloads[i] = payload;
          modified = true;
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
    var u = "" + url;
    try {
      var m = /[?&]access_token=([^&]+)/.exec(u);
      if (m) wsAccessToken = m[1];
    } catch (e) {}

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

  // --- Ad Detection ---

  function isAdTrack(track) {
    if (!track || !track.metadata) return false;
    var uri = track.metadata.uri || "";
    return uri.includes(":ad:") || track.content_type === "AD";
  }

  function isAdState(state, stateMachine) {
    if (!state) return false;
    var track = stateMachine.tracks[state.track];
    return isAdTrack(track);
  }

  // --- State Machine Manipulation ---

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

  async function manipulateStateMachine(stateMachine, startingStateIndex) {
    if (!stateMachine || !stateMachine.states || !stateMachine.tracks) return stateMachine;

    var states = stateMachine.states;
    var tracks = stateMachine.tracks;

    for (var i = 0; i < states.length; i++) {
      var state = states[i];
      if (!isAdState(state, stateMachine)) continue;

      var track = tracks[state.track];
      var uri = (track && track.metadata && track.metadata.uri) || "unknown";

      var nextState = getNextNonAdState(stateMachine, i);

      if (nextState) {
        var replacement = JSON.parse(JSON.stringify(nextState));
        replacement.state_id = state.state_id;
        states[i] = replacement;
        totalAdsRemoved++;
        console.log("[unspot] removed ad: " + uri);
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
            totalAdsRemoved++;
            console.log("[unspot] removed ad (fetched more): " + uri);
          } else {
            states[i] = shortenState(state, track);
            console.log("[unspot] shortened ad: " + uri);
          }
        } else {
          states[i] = shortenState(state, track);
          console.log("[unspot] shortened ad (no auth): " + uri);
        }
      }
    }

    stateMachine.states = states;
    stateMachine.tracks = tracks;
    return stateMachine;
  }

  // --- Fetch Hook ---

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
            var idx = (data.updated_state_ref && data.updated_state_ref.state_index) || 0;
            data.state_machine = await manipulateStateMachine(data.state_machine, idx);
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