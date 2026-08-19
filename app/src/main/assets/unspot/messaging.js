const logErr = (ctx, e) =>
  window.postMessage(
    {
      direction: "from-page-script",
      message: {
        type: "log",
        level: "error",
        text: `[messaging] ${ctx}: ${(e?.stack ?? e?.message ?? String(e)).toString().slice(0, 1500)}`,
      },
    },
    "*",
  );

let Platform;
(() => {
  const CHUNK_GLOBALS = [
    "rspackChunk",
    "rspackChunkclient_web",
    "webpackChunkclient_web",
  ];

  const definePlatform = () => {
    Object.defineProperty(window, "Platform", {
      value: Object.getOwnPropertyNames(Platform).reduce(
        (p, c) => ({ ...p, [c]: Platform[c] }),
        {},
      ),
    });
  };

  const installHook = (require) => {
    const originalD = require.d.bind(require);
    require.d = (exports, descriptors) => {
      if (Object.hasOwn(descriptors, "createPlatformWeb")) {
        const originalGetter = descriptors.createPlatformWeb;
        descriptors = Object.assign({}, descriptors, {
          createPlatformWeb: () => {
            const originalFn = originalGetter();
            return async function createPlatformWeb(...args) {
              const platform = await originalFn.apply(this, args);
              window._Platform = platform;
              definePlatform();
              return platform;
            };
          },
        });
      }
      originalD(exports, descriptors);
    };
  };

  for (const name of CHUNK_GLOBALS) {
    window[name] = window[name] || [];
    window[name].push([["__platform_hook__"], {}, installHook]);
  }

  Platform = new Proxy(
    {},
    {
      get: (_, prop) => {
        if (!window._Platform) return undefined;
        if (prop === "then") return Promise.resolve(window._Platform);
        const entry = window._Platform.getRegistry()._map.get(Symbol.for(prop));
        return entry?.instance;
      },
      ownKeys: () => {
        if (!window._Platform) return [];
        return [
          ...window._Platform
            .getRegistry()
            ._map.keys()
            .map((k) => Symbol.keyFor(k)),
        ];
      },
    },
  );
  window.Platform = Platform;
})();

const _npb = document.querySelector("aside");

function typeText(input, text) {
  input.focus();
  const nativeSetter = Object.getOwnPropertyDescriptor(
    HTMLInputElement.prototype,
    "value",
  ).set;
  let current = "";
  for (const char of text) {
    current += char;
    input.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: char,
        bubbles: true,
      }),
    );
    nativeSetter.call(input, current);
    input.dispatchEvent(
      new Event("input", {
        bubbles: true,
      }),
    );
    input.dispatchEvent(
      new KeyboardEvent("keyup", {
        key: char,
        bubbles: true,
      }),
    );
  }
  input.dispatchEvent(new Event("change", { bubbles: true }));
}

const handlers = {
  getIsSignedIn: () => {
    return document.cookie.includes("sp_key");
  },
  goToLogin: () => {
    if (window.location.href.includes("login?")) return true;
    window.location.href =
      "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F&allow_password=1";
    return true;
  },
  login: (data) => {
    const { email, password } = data;
    const emailInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const loginButton = document.querySelector('button[type="submit"]');

    if (emailInput && passwordInput && loginButton) {
      typeText(emailInput, email);
      typeText(passwordInput, password);
      loginButton.click();
      return { success: true };
    }
    return { success: false, error: "Inputs not found" };
  },

  getPlaybackState: async () => {
    const state = await Platform.PlayerSDK.harmony.getCurrentState();
    if (!state) return null;
    return {
      title: state.track_window.current_track.name,
      artist: state.track_window.current_track.artists
        .map((a) => a.name)
        .join(", "),
      albumArt: state.track_window.current_track.album.images.reduce(
        (acc, img) => (img.height > acc.height ? img : acc),
      ).url,
      isPlaying: !state.paused,
      currentTime: state.position,
      totalTime: state.track_window.current_track.duration_ms,
    };
  },
  getPlaybackProgress: async () => {
    const state = await Platform.PlayerSDK.harmony.getCurrentState();
    if (!state) return null;
    return {
      currentTime: state.position,
    };
  },

  resumePlayback: async () => {
    await Platform.PlayerSDK.harmony.resume();
  },
  pausePlayback: async () => {
    await Platform.PlayerSDK.harmony.pause();
  },
  skipTrack: async () => {
    await Platform.PlayerSDK.harmony.nextTrack();
    return { success: true };
  },
  previousTrack: async () => {
    const { position } = await Platform.PlayerSDK.harmony.getCurrentState();
    if (position <= 5000) {
      await Platform.PlayerSDK.harmony.previousTrack();
    } else {
      await Platform.PlayerSDK.harmony.seek(0);
    }
    return { success: true };
  },
  setPlaybackPosition: async (data) => {
    const { position } = data;
    await Platform.PlayerSDK.harmony.seek(position);
    return { success: true };
  },
  play: async ({ contextUri, trackUri }) => {
    await Platform.PlayerAPI._harmony.playURI(contextUri, null, {
      contextURI: contextUri,
      trackURI: trackUri,
    });
    return { success: true };
  },

  getLibraryData: async () => {
    const [{ items }, currentState] = await Promise.all([
      await Platform.LibraryAPI.getContents(),
      await Platform.PlayerSDK.harmony.getCurrentState(),
    ]);
    if (items === null) return null;
    const getSubtitle = (item) => {
      switch (item.type) {
        case "album":
          return `Album • ${item.artists[0].name}`;
        case "artist":
          return "Artist";
        case "playlist":
          return `Playlist • ${item.owner.name}`;
        default:
          return "";
      }
    };
    return {
      items: items.map((item) => ({
        uri: item.uri,
        type: item.type,
        cover: item.images.reduce((acc, img) =>
          img.height < acc.height ? img : acc,
        ).url,
        title: item.name,
        subtitle: getSubtitle(item),
        isActive: item.uri === (currentState?.context?.uri ?? ""),
      })),
    };
  },

  getPlaylist: async ({ uri }) => {
    const playlist = await Platform.PlaylistAPI.getPlaylistMetadata(uri);
    return {
      uri: playlist.uri,
      cover: playlist.images.reduce((acc, img) =>
        img.height > acc.height ? img : acc,
      ).url,
      title: playlist.name,
      description: playlist.description,
      duration: playlist.duration.milliseconds,
      length: playlist.unfilteredTotalLength,
      collaborators: playlist.collaborators.items.map((c) => ({
        name: c.user.displayName,
        uri: c.user.uri,
        image: c.user.images.reduce((acc, img) =>
          img.height < acc.height ? img : acc,
        ).url,
      })),
    };
  },
  getPlaylistContent: async ({ uri, offset }) => {
    const contents = await Platform.PlaylistAPI.getPlaylistContents(uri, {
      limit: 50,
      offset: offset ?? 0,
    });
    contents.items = contents.items.map((item) => ({
      index: offset + item.playIndex,
      uri: item.uri,
      type: item.type,
      cover: item.album.images.reduce((acc, img) =>
        img.height < acc.height ? img : acc,
      ).url,
      title: item.name,
      subtitle: item.artists.map((a) => a.name).join(", "),
      duration: item.duration.milliseconds,
    }));
    return contents;
  },
};

const postMessage = (message) => {
  window.postMessage(
    {
      direction: "from-page-script",
      message,
    },
    "*",
  );
};
window.addEventListener("message", async (event) => {
  if (
    event.source === window &&
    event?.data?.direction === "from-content-script"
  ) {
    const { message } = event.data;

    const handler = handlers[message.type];
    if (handler) {
      try {
        const result = await handler(message.data);
        if (result !== undefined) {
          postMessage({ type: `${message.type}Response`, data: result });
        }
      } catch (e) {
        postMessage({ type: `${message.type}Response`, error: e.message });
      }
    }
  }
});

const postLibraryUpdate = async () => {
  const result = await handlers.getLibraryData();
  if (!result) return;
  postMessage({ type: "getLibraryDataResponse", data: result });
};
(async () => {
  while (!Platform?.LibraryAPI?._events?._emitter?._listeners?.update) {
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
  Platform.LibraryAPI._events._emitter._listeners.update.push({
    listener: postLibraryUpdate,
    options: {},
  });
  postLibraryUpdate();
})();

(async () => {
  while (!Platform?.PlayerSDK?.harmony?._listeners?.seek_panels_loaded) {
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
  Platform.PlayerSDK.harmony._listeners.seek_panels_loaded.push({
    listener: async () => {
      const state = await handlers.getPlaybackState();
      if (!state) return;
      postMessage({ type: "playbackStateUpdate", data: state });
    },
    options: {},
  });
  Platform.PlayerSDK.harmony._controller._listeners.state_changed.push({
    listener: async () => {
      const state = await handlers.getPlaybackState();
      if (!state) return;
      postMessage({ type: "playbackStateUpdate", data: state });
      postLibraryUpdate();
    },
    options: {},
  });

  let lastFullStateUpdate = 0;
  Platform.PlayerSDK.harmony._controller._listeners.progress.push({
    listener: async () => {
      if (Date.now() - lastFullStateUpdate > 10000) {
        const state = await handlers.getPlaybackState();
        if (!state) return;
        postMessage({ type: "playbackStateUpdate", data: state });
        lastFullStateUpdate = Date.now();
      } else {
        const progress = await handlers.getPlaybackProgress();
        if (!progress) return;
        postMessage({ type: "playbackProgressUpdate", data: progress });
      }
    },
    options: {},
  });

  const noop = () => {};
  window.MessageChannel = function () {
    this.port1 = this.port2 = {
      onmessage: null,
      onmessageerror: null,
      postMessage: noop,
      close: noop,
      start: noop,
      addEventListener: noop,
      removeEventListener: noop,
      dispatchEvent: () => true,
    };
  };
})();

(async () => {
  while (!Platform?.PlayerSDK?.harmony?._client?._deviceDescriptor) {
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
  const originalDeviceDescriptor =
    Platform.PlayerSDK.harmony._client._deviceDescriptor;
  Platform.PlayerSDK.harmony._client._deviceDescriptor = new Promise(
    (resolve) => {
      originalDeviceDescriptor.then((deviceDescriptor) => {
        deviceDescriptor._name = "Unspot";
        resolve(deviceDescriptor);
      });
    },
  );
})();

// Disable Harmony ad managers via the SDK's AdManagers service.
(async () => {
  try {
    while (!Platform?.AdManagers?.audio?.disable) {
      await new Promise((r) => setTimeout(r, 500));
    }
    const adManagers = Platform.AdManagers;

    const disableManager = (manager) => {
      if (!manager) return;
      try {
        if (typeof manager.disable === "function") manager.disable();
        else if (typeof manager.disableLeaderboard === "function")
          manager.disableLeaderboard();
        else if ("enabled" in manager) manager.enabled = false;
      } catch (e) {
        logErr("disableManager", e);
      }
    };

    const managerPaths = [
      "audio",
      "inStreamApi",
      "sponsoredPlaylist",
      "leaderboard",
      "hpto",
      "home",
      "survey",
      "vto.manager",
      "embeddedAd.embeddedAdManager",
      "embeddedPlaylist.embeddedPlaylistManager",
    ];
    for (const path of managerPaths) {
      let node = adManagers;
      let ok = true;
      for (const key of path.split(".")) {
        if (!node || typeof node !== "object") {
          ok = false;
          break;
        }
        node = node[key];
      }
      if (ok) disableManager(node);
    }

    const connector = adManagers.audio?.inStreamApi?.adsCoreConnector;
    const SLOT_IDS = [
      "preroll",
      "stream",
      "embedded-npv",
      "embedded-playlist-leavebehind",
      "embedded-playlist",
      "hpto",
      "leaderboard",
      "podcast-midroll-1",
      "podcast-midroll-2",
      "podcast-midroll-3",
      "podcast-midroll-4",
      "podcast-midroll-5",
      "podcast-postroll",
      "podcast-preroll",
    ];

    const clearSlot = (slotId) => {
      try {
        if (connector && typeof connector.clearSlot === "function")
          connector.clearSlot(slotId);
      } catch (_e) {}
    };
    for (const slot of SLOT_IDS) clearSlot(slot);
    if (connector && typeof connector.subscribeToSlot === "function") {
      for (const slot of SLOT_IDS) {
        try {
          connector.subscribeToSlot(slot, () => clearSlot(slot));
        } catch (_e) {}
      }
    }

    setInterval(() => {
      try {
        if (adManagers.audio && adManagers.audio.enabled !== false)
          disableManager(adManagers.audio);
      } catch (e) {
        logErr("adInterval", e);
      }
    }, 10000);
  } catch (e) {
    logErr("adManagersInit", e);
  }
})();
