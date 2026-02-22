let _Platform;

window.webpackChunkclient_web = window.webpackChunkclient_web || [];

window.webpackChunkclient_web.push([
  ["__platform_hook__"],
  {},
  function installHook(require) {
    const originalD = require.d.bind(require);
    require.d = function (exports, descriptors) {
      if (Object.prototype.hasOwnProperty.call(descriptors, "createPlatformWeb")) {
        const originalGetter = descriptors.createPlatformWeb;
        descriptors = Object.assign({}, descriptors, {
          createPlatformWeb: function () {
            const originalFn = originalGetter();
            return async function createPlatformWeb(...args) {
              const platform = await originalFn.apply(this, args);
              _Platform = platform;
              return platform;
            };
          },
        });
      }
      originalD(exports, descriptors);
    };
  },
]);

const Platform = new Proxy(
  {},
  {
    get: function (_, prop) {
      if (!_Platform) return undefined;
      if (prop === "then") return Promise.resolve(_Platform);
      return _Platform.getRegistry()._map.get(Symbol.for(prop)).instance;
    },
    ownKeys: function () {
      if (!_Platform) return [];
      return [
        ..._Platform
          .getRegistry()
          ._map.keys()
          .map((k) => Symbol.keyFor(k)),
      ];
    },
  },
);
window.Platform = Platform;

let npb = document.querySelector("aside");

function typeText(input, text) {
  input.focus();
  const nativeSetter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set;
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

function getPlaybackState() {
  if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };

  const title = npb.querySelector("div[data-testid='context-item-info-title'] a")?.innerText ?? "";
  const artist = npb.querySelector("div[data-testid='context-item-info-subtitles'] a")?.innerText ?? "";
  const albumArtSm = npb.querySelector("button[data-testid='cover-art-button'] img")?.src ?? "";
  const albumArt = albumArtSm.replace(/(?<=0000)[0-9a-f]{4}/, "b273");
  const isPlaying = getIsPlaying();

  const playProgressBar = npb.querySelector("div[data-testid='playback-progressbar'] input");
  const currentTime = +playProgressBar?.getAttribute("value") ?? 0;
  const totalTime = +playProgressBar?.getAttribute("max") ?? 0;

  return {
    title,
    artist,
    albumArt,
    isPlaying,
    currentTime,
    totalTime,
  };
}
function getIsPlaying() {
  const playpause = npb.querySelector("button[data-testid='control-button-playpause'] svg > path");
  if (!playpause) return false;
  return playpause.getAttribute("d")?.toLowerCase()?.match(/z/g)?.length === 2;
}

const handlers = {
  getIsSignedIn: () => {
    return document.cookie.includes("sp_key");
  },
  goToLogin: () => {
    if (window.location.href.includes("login?")) return true;
    window.location.href = "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F&allow_password=1";
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
      await Platform.PlayerSDK.harmony.seek(0);
    } else {
      await Platform.PlayerSDK.harmony.previousTrack();
    }
    return { success: true };
  },
  setPlaybackPosition: async (data) => {
    const { position } = data;
    await Platform.PlayerSDK.harmony.seek(position);
    return { success: true };
  },

  getLibraryData: async () => {
    const [{ items }, { context }] = await Promise.all([await Platform.LibraryAPI.getContents(), await Platform.PlayerSDK.harmony.getCurrentState()]);
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
        cover: item.images.reduce((cur, prev) => (cur.width < prev.width ? cur : prev)).url,
        title: item.name,
        subtitle: getSubtitle(item),
        isActive: item.uri === (context?.uri ?? ""),
      })),
    };
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
  if (event.source === window && event?.data?.direction === "from-content-script") {
    const { message } = event.data;

    console.log("Received message from app:", message);
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

const npbObserver = new MutationObserver(() => {
  const state = getPlaybackState();
  postMessage({ type: "playbackStateUpdate", data: state });
});
const npbFinder = new MutationObserver(() => {
  npb = document.querySelector("aside");
  if (npb) {
    npbObserver.observe(npb, { childList: true, subtree: true, attributes: true });
    npbFinder.disconnect();
  }
});
npbFinder.observe(document.documentElement, { childList: true, subtree: true });

const postLibraryUpdate = async () => {
  postMessage({ type: "getLibraryDataResponse", data: await handlers.getLibraryData() });
};
const libraryObserver = new MutationObserver(() => {
  postLibraryUpdate();
});
const libraryFinder = new MutationObserver(() => {
  const library = document.querySelector("nav .YourLibraryX [role='presentation']:not([data-testid])");
  if (library) {
    libraryObserver.observe(library, { childList: true, subtree: true });
    libraryFinder.disconnect();
    postLibraryUpdate();
  }
});
libraryFinder.observe(document.documentElement, { childList: true, subtree: true });
