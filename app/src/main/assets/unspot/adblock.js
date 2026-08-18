(() => {
let authorization = "";
let deviceId = "";
const originalFetch = window.fetch;

const isAdMedia = (el) => {
  const src = String(el.currentSrc || el.src || "");
  if (/:ad:/.test(src)) return true;
  if (src.includes("adstudio") || src.includes("audio-ads")) return true;
  const dur = el.duration || 0;
  // i like my magic numbers
  if (dur > 0 && dur < 67) return true;
  return false;
};

const muteEl = (el) => {
  try { el.muted = true; } catch (_e) {}
  try { el.volume = 0; } catch (_e) {}
  try {
    Object.defineProperty(el, "volume", { configurable: true, get: () => 0, set: () => {} });
    Object.defineProperty(el, "muted", { configurable: true, get: () => true, set: () => {} });
  } catch (_e) {}
  if (el.setAttribute) { try { el.setAttribute("muted", ""); } catch (_e) {} }
};

const killAd = (el) => {
  muteEl(el);
  const dur = el.duration || 0;
  try { el.currentTime = dur; } catch (_e) {}
  try { el.pause(); } catch (_e) {}
  try { el.dispatchEvent(new Event("ended", { bubbles: false })); } catch (_e) {}
};

  try {
    const origCreate = document.createElement;
    document.createElement = function (_tag) {
      const el = origCreate.apply(this, arguments);
      if (el instanceof HTMLMediaElement) {
        el.addEventListener("play", function () {
          if (isAdMedia(this)) killAd(this);
        }, true);
        el.addEventListener("loadedmetadata", function () {
          if (isAdMedia(this)) { try { this.pause(); } catch (_e) {} }
        }, true);
        el.addEventListener("timeupdate", function () {
          if ((this.duration || 0) > 0 && (this.duration || 0) < 67 && !this.paused) killAd(this);
        }, true);
        const oldPlay = el.play;
        el.play = function () {
          if (isAdMedia(this)) { killAd(this); return Promise.resolve(); }
          return oldPlay.apply(this, arguments);
        };
        // setAttribute("src", adUrl) bypasses the src property setter
        const oldSetAttr = el.setAttribute;
        el.setAttribute = function (name, value) {
          if (name === "src") {
            const s = String(value || "");
            if (/adstudio/i.test(s) || /audio-ads/i.test(s) || /audio-fa\.scdn\.co/i.test(s)) {
              muteEl(this);
            }
          }
          return oldSetAttr.apply(this, arguments);
        };
      }
      return el;
    };
  } catch (_e) {}

  try {
    const origProtoPlay = HTMLMediaElement.prototype.play;
    HTMLMediaElement.prototype.play = function () {
      if (isAdMedia(this)) { killAd(this); return Promise.resolve(); }
      return origProtoPlay.apply(this, arguments);
    };
    document.addEventListener("play", (e) => {
      const t = e.target;
      if (t?.duration && t.duration < 67 && t.duration > 0) killAd(t);
    }, true);
    document.addEventListener("loadedmetadata", (e) => {
      const t = e.target;
      if (t?.duration && t.duration < 67 && t.duration > 0) killAd(t);
    }, true);
    document.addEventListener("timeupdate", (e) => {
      const t = e.target;
      if (t && t instanceof HTMLMediaElement && !t.paused && isAdMedia(t)) killAd(t);
    }, true);
  } catch (_e) {}

  try {
    const origSrcDesc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, "src");
    Object.defineProperty(HTMLMediaElement.prototype, "src", {
      configurable: true,
      get: function () { return origSrcDesc.get.call(this); },
      set: function (v) {
        const s = String(v || "");
        const isAdLike = /audio-fa\.scdn\.co\/audio/i.test(s) || /adstudio/i.test(s) || /audio-ads/i.test(s) || /\.scdn\.co\/mp3\//i.test(s) || /^spotify-audio:\/\//i.test(s);
        if (isAdLike && !s.startsWith("blob:")) {
          muteEl(this);
          const guard = setInterval(() => { muteEl(this); }, 300);
          const stopGuard = () => {
            clearInterval(guard);
            this.removeEventListener("ended", stopGuard);
            this.removeEventListener("error", stopGuard);
          };
          this.addEventListener("ended", stopGuard);
          this.addEventListener("error", stopGuard);
          return origSrcDesc.set.call(this, v);
        }
        return origSrcDesc.set.call(this, v);
      }
    });
  } catch (_e) {}

  setInterval(() => {
    try {
      let scanEls;
      function collectSc(root) {
        const w = root.querySelectorAll ? root.querySelectorAll("audio,video") : [];
        for (let i = 0; i < w.length; i++) scanEls.push(w[i]);
        const hosts = root.querySelectorAll ? root.querySelectorAll("*") : [];
        for (let j = 0; j < hosts.length; j++) {
          if (hosts[j].shadowRoot) collectSc(hosts[j].shadowRoot);
        }
      }
      scanEls = [];
      collectSc(document);
      for (let i = 0; i < scanEls.length; i++) {
        const el = scanEls[i];
        if (isAdMedia(el) && !el.paused) killAd(el);
      }
    } catch (_e) {}
  }, 200);

  function processWsMessage(event) {
    try {
      const data = JSON.parse(event.data);
      if (!data.payloads) return event;

      let modified = false;
      for (let i = 0; i < data.payloads.length; i++) {
        const payload = data.payloads[i];

        // Drop ad payload
        if (payload.cluster?.player_state?.track) {
          const ctrack = payload.cluster.player_state.track;
          const adCluster = /:ad:/.test(String(ctrack.uri || "")) ||
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
          const sm = payload.state_machine;
          if (sm?.states && sm.tracks) {
            for (let j = 0; j < sm.states.length; j++) {
              if (isAdState(sm.states[j], sm)) {
                const next = getNextNonAdState(sm, j);
                if (next) {
                  const rep = JSON.parse(JSON.stringify(next));
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
    } catch (_e) {
      return event;
    }
  }

  const _WS = WebSocket;
  // must be real function for constructor to exist
  WebSocket = function WebSocket(url, protocols) {
    const ws = protocols ? new _WS(url, protocols) : new _WS(url);
    let _origOnMessage = null;

    ws.addEventListener("message", (event) => {
      if (!_origOnMessage) return;
      const processed = processWsMessage(event);
      _origOnMessage.call(ws, processed);
    });

    Object.defineProperty(ws, "onmessage", {
      get: () => _origOnMessage,
      set: (fn) => { _origOnMessage = fn; }
    });

    const _origAddEventListener = ws.addEventListener.bind(ws);
    ws.addEventListener = (type, listener, options) => {
      if (type === "message" && typeof listener === "function") {
        const wrappedListener = (event) => {
          const processed = processWsMessage(event);
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
    if (!track?.metadata) return false;
    const uri = track.metadata.uri || track.uri || "";
    return uri.includes(":ad:") || track.content_type === "AD";
  }

  function isAdState(state, stateMachine) {
    if (!state) return false;
    const track = stateMachine.tracks[state.track];
    return isAdTrack(track);
  }

  function getNextNonAdState(stateMachine, fromIndex) {
    const states = stateMachine.states;
    const visited = {};
    let idx = fromIndex;
    let maxIter = states.length + 1;
    while (maxIter-- > 0) {
      const state = states[idx];
      if (!state) return null;
      const advance = state.transitions?.advance;
      if (!advance) return null;
      const nextIdx = advance.state_index;
      if (visited[nextIdx]) return null;
      visited[nextIdx] = true;
      const nextState = states[nextIdx];
      if (!nextState) return null;
      if (!isAdState(nextState, stateMachine)) return nextState;
      idx = nextIdx;
    }
    return null;
  }

  function shortenState(state, track) {
    const duration = (track?.metadata?.duration) || 0;
    state.disallow_seeking = false;
    state.restrictions = {};
    state.initial_playback_position = duration;
    state.position_offset = duration;
    return state;
  }

  async function fetchMoreStates(stateMachineId, stateId) {
    if (!authorization || !deviceId) return null;
    const url = `https://spclient.wg.spotify.com/track-playback/v1/devices/${deviceId}/state`;
    const body = JSON.stringify({
      seq_num: Date.now(),
      state_ref: { state_machine_id: stateMachineId, state_id: stateId, paused: false },
      sub_state: { playback_speed: 1, position: 0, duration: 0, stream_time: 0, media_type: "AUDIO", bitrate: 160000 },
      previous_position: 0,
      debug_source: "resume"
    });
    try {
      const resp = await originalFetch.call(window, url, {
        method: "PUT",
        headers: { "Authorization": authorization, "Content-Type": "application/json" },
        body: body
      });
      if (resp.status !== 200) return null;
      const data = await resp.json();
      return data.state_machine || null;
    } catch (_e) { return null; }
  }

  async function manipulateStateMachine(stateMachine) {
    if (!stateMachine?.states || !stateMachine.tracks) return stateMachine;

    const states = stateMachine.states;
    const tracks = stateMachine.tracks;

    for (let i = 0; i < states.length; i++) {
      const state = states[i];
      if (!isAdState(state, stateMachine)) continue;

      const track = tracks[state.track];
      const nextState = getNextNonAdState(stateMachine, i);

      if (nextState) {
        const replacement = JSON.parse(JSON.stringify(nextState));
        replacement.state_id = state.state_id;
        states[i] = replacement;
      } else {
        const fetched = await fetchMoreStates(
          stateMachine.state_machine_id,
          state.state_id
        );
        if (fetched) {
          let fetchedNext = null;
          for (let j = 0; j < (fetched.states || []).length; j++) {
            const fs = fetched.states[j];
            const ft = fetched.tracks[fs.track];
            if (!isAdTrack(ft)) { fetchedNext = fs; break; }
          }
          if (fetchedNext) {
            const newTrackIdx = tracks.length;
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

  window.fetch = (url, init) => {
    const urlStr = typeof url === "string" ? url : (url?.url) || "";
    const method = (init?.method) || (url?.method) || "GET";

    if (urlStr.includes("spclient.wg.spotify.com")) {
      let h = null;
      if (init?.headers) {
        h = init.headers;
      } else if (url && typeof url !== "string" && url.headers) {
        h = url.headers;
      }
      if (h) {
        const auth = h.authorization || h.Authorization || (h.get?.("authorization"));
        if (auth) authorization = auth;
      }
    }

    if (urlStr.endsWith("/devices") && init?.body) {
      try {
        const parsed = JSON.parse(init.body);
        if (parsed.device?.device_id) {
          deviceId = parsed.device.device_id;
        }
      } catch (_e) {}
    }

    if (method.toUpperCase() === "PUT" && urlStr.endsWith("/state") && urlStr.includes("spclient.wg.spotify.com")) {
      return originalFetch.call(window, url, init).then((response) => {
        const clone = response.clone();
        return response.json().then(async (data) => {
          if (!data?.state_machine) return clone;
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
        }).catch(() => clone);
      });
    }

    return originalFetch.call(window, url, init);
  };
})();
