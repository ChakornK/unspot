const ipc = browser.runtime.connectNative("browser");

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
  const currentTime = +playProgressBar?.value ?? 0;
  const totalTime = +playProgressBar?.max ?? 0;

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
  togglePlayback: () => {
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };
    npb.querySelector("button[data-testid='control-button-playpause']").click();

    return { success: true };
  },
  skipTrack: () => {
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };
    npb.querySelector("button[data-testid='control-button-skip-forward']").click();
    return { success: true };
  },
  previousTrack: () => {
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };
    npb.querySelector("button[data-testid='control-button-skip-back']").click();
    return { success: true };
  },
  setPlaybackPosition: (data) => {
    const { position } = data;
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };
    npb.querySelector("div[data-testid='playback-progressbar'] input").value = position;
    npb.querySelector("div[data-testid='playback-progressbar'] input").dispatchEvent(new Event("input", { bubbles: true }));
    return { success: true };
  },
};

ipc.onMessage.addListener((message) => {
  console.log("Received message from app:", message);
  const handler = handlers[message.type];
  if (handler) {
    try {
      const result = handler(message.data);
      if (result !== undefined) {
        ipc.postMessage({ type: `${message.type}Response`, data: result });
      }
    } catch (e) {
      ipc.postMessage({ type: `${message.type}Response`, error: e.message });
    }
  }
});

const npbObserver = new MutationObserver(() => {
  const state = getPlaybackState();
  ipc.postMessage({ type: "playbackStateUpdate", data: state });
});
const npbFinder = new MutationObserver(() => {
  npb = document.querySelector("aside");
  if (npb) {
    npbObserver.observe(npb, { childList: true, subtree: true, attributes: true });
    npbFinder.disconnect();
  }
});
npbFinder.observe(document.documentElement, { childList: true, subtree: true });
