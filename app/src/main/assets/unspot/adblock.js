(() => {
  let authorization = "";
  let deviceId = "";
  const originalFetch = window.fetch;
  const logErr = (ctx, e) =>
    window.postMessage(
      {
        direction: "from-page-script",
        message: {
          type: "log",
          level: "error",
          text: `[adblock] ${ctx}: ${(e?.stack ?? e?.message ?? String(e)).toString().slice(0, 1500)}`,
        },
      },
      "*",
    );

  const originalCreateElement = document.createElement;
  document.createElement = (() => {
    return function () {
      const element = originalCreateElement.apply(this, arguments);
      if (element instanceof HTMLMediaElement) {
        const oldPlay = element.play;
        element.play = function () {
          if (
            !this.src.startsWith("blob:https://open.spotify.com/") &&
            this.duration < 67
          ) {
            this.dispatchEvent(new Event("play"));
            const oldSrc = this.src.toString();
            setTimeout(() => {
              this.currentTime = this.duration;
              this.dispatchEvent(new Event("timeupdate"));
              const inter = setInterval(() => {
                if (this.src !== oldSrc) {
                  this.currentTime = 0;
                  return clearInterval(inter);
                }
                this.currentTime = this.duration;
                this.dispatchEvent(new Event("ended"));
              }, 400);
              setTimeout(() => {
                try {
                  clearInterval(inter);
                } catch (_e) {}
              }, 2000);
            }, 20);
          } else {
            oldPlay.apply(this, arguments);
          }
        };
      }
      return element;
    };
  })();

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
          const adCluster =
            /:ad:/.test(String(ctrack.uri || "")) ||
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
          if (sm?.states && sm.tracks && rewireAds(sm)) {
            modified = true;
          }
        }
      }

      if (modified) {
        return new MessageEvent(event.type, {
          data: JSON.stringify(data),
          origin: event.origin,
          lastEventId: event.lastEventId,
          source: event.source,
          ports: event.ports,
        });
      }
      return event;
    } catch (e) {
      logErr("processWsMessage", e);
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
      set: (fn) => {
        _origOnMessage = fn;
      },
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
    if (!track) return false;
    const meta = track.metadata || {};
    const uri = String(meta.uri || track.uri || "");
    const provider = String(meta.provider || track.provider || "");
    const contentType = String(track.content_type || "").toUpperCase();
    return (
      uri.includes(":ad:") ||
      uri.startsWith("ads/") ||
      provider.startsWith("ads/") ||
      contentType === "AD"
    );
  }

  function isAdState(state, stateMachine) {
    if (!state) return false;
    const track = stateMachine.tracks[state.track];
    return isAdTrack(track);
  }

  function nextNonAdIndex(stateMachine, fromIndex) {
    const states = stateMachine.states;
    const visited = {};
    let idx = fromIndex;
    let maxIter = states.length + 1;
    while (maxIter-- > 0) {
      const state = states[idx];
      if (!state) return null;
      const advance = state.transitions?.advance;
      if (!advance || typeof advance.state_index !== "number") return null;
      if (visited[advance.state_index]) return null;
      visited[advance.state_index] = true;
      idx = advance.state_index;
      const nextState = states[idx];
      if (!nextState) return null;
      if (!isAdState(nextState, stateMachine)) return idx;
    }
    return null;
  }

  function shortenState(state, track) {
    const duration = track?.metadata?.duration || 0;
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
      state_ref: {
        state_machine_id: stateMachineId,
        state_id: stateId,
        paused: false,
      },
      sub_state: {
        playback_speed: 1,
        position: 0,
        duration: 0,
        stream_time: 0,
        media_type: "AUDIO",
        bitrate: 160000,
      },
      previous_position: 0,
      debug_source: "resume",
    });
    try {
      const resp = await originalFetch.call(window, url, {
        method: "PUT",
        headers: {
          Authorization: authorization,
          "Content-Type": "application/json",
        },
        body: body,
      });
      if (resp.status !== 200) return null;
      const data = await resp.json();
      return data.state_machine || null;
    } catch (e) {
      logErr("fetchMoreStates", e);
      return null;
    }
  }

  function rewireAds(stateMachine) {
    const states = stateMachine.states;
    const succ = new Array(states.length);
    for (let i = 0; i < states.length; i++)
      succ[i] = nextNonAdIndex(stateMachine, i);
    let modified = false;
    for (let i = 0; i < states.length; i++) {
      const state = states[i];
      const ad = isAdState(state, stateMachine);
      const target = succ[i];
      if (target != null) {
        if (!state.transitions) state.transitions = {};
        const tr = state.transitions;
        tr.advance = tr.advance || {};
        if (tr.advance.state_index !== target) {
          tr.advance.state_index = target;
          modified = true;
        }
        if (ad) {
          shortenState(state, stateMachine.tracks[state.track]);
          modified = true;
        }
      } else if (ad) {
        shortenState(state, stateMachine.tracks[state.track]);
        modified = true;
      }
    }
    return modified;
  }

  async function manipulateStateMachine(stateMachine) {
    if (!stateMachine?.states || !stateMachine.tracks) return stateMachine;
    for (let i = 0; i < stateMachine.states.length; i++) {
      const state = stateMachine.states[i];
      if (!isAdState(state, stateMachine)) continue;
      if (nextNonAdIndex(stateMachine, i) != null) continue;
      const adv = state.transitions?.advance;
      const tail =
        !adv ||
        typeof adv.state_index !== "number" ||
        !stateMachine.states[adv.state_index];
      if (!tail) continue;
      const fetched = await fetchMoreStates(
        stateMachine.state_machine_id,
        state.state_id,
      );
      if (!fetched?.states || !fetched.tracks) continue;
      let fs = null;
      for (let j = 0; j < fetched.states.length; j++) {
        const cand = fetched.states[j];
        if (cand && !isAdTrack(fetched.tracks[cand.track])) {
          fs = cand;
          break;
        }
      }
      if (!fs) continue;
      const newTrackIdx = stateMachine.tracks.length;
      stateMachine.tracks.push(fetched.tracks[fs.track]);
      const clone = JSON.parse(JSON.stringify(fs));
      clone.track = newTrackIdx;
      clone.transitions = {};
      stateMachine.states.push(clone);
      const newIdx = stateMachine.states.length - 1;
      if (!state.transitions) state.transitions = {};
      const tr = state.transitions;
      tr.advance = tr.advance || {};
      tr.advance.state_index = newIdx;
    }
    rewireAds(stateMachine);
    return stateMachine;
  }

  window.fetch = (url, init) => {
    const urlStr = typeof url === "string" ? url : url?.url || "";
    const method = init?.method || url?.method || "GET";

    if (urlStr.includes("spclient.wg.spotify.com")) {
      let h = null;
      if (init?.headers) {
        h = init.headers;
      } else if (url && typeof url !== "string" && url.headers) {
        h = url.headers;
      }
      if (h) {
        const auth =
          h.authorization || h.Authorization || h.get?.("authorization");
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

    if (
      method.toUpperCase() === "PUT" &&
      urlStr.endsWith("/state") &&
      urlStr.includes("spclient.wg.spotify.com")
    ) {
      return originalFetch.call(window, url, init).then((response) => {
        const clone = response.clone();
        return response
          .json()
          .then(async (data) => {
            if (!data?.state_machine) return clone;
            try {
              data.state_machine = await manipulateStateMachine(
                data.state_machine,
              );
              return new Response(JSON.stringify(data), {
                status: response.status,
                statusText: response.statusText,
                headers: response.headers,
              });
            } catch (e) {
              logErr("fetchManipulate", e);
              return clone;
            }
          })
          .catch(() => clone);
      });
    }

    return originalFetch.call(window, url, init);
  };
})();
