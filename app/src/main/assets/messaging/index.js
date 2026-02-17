const ipc = browser.runtime.connectNative("browser");

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

function getIsPlaying(npb) {
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
  getPlaybackState: () => {
    const npb = document.querySelector("aside");
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };

    const title = npb.querySelector("div[data-testid='context-item-info-title'] a")?.innerText ?? "";
    const artist = npb.querySelector("div[data-testid='context-item-info-subtitles'] a")?.innerText ?? "";
    const albumArt = npb.querySelector("button[data-testid='cover-art-button'] img")?.src ?? "";
    const fillResAlbumArt = albumArt.replace(/(?<=0000)[0-9a-f]{4}/, "b273");
    const isPlaying = getIsPlaying(npb);

    return {
      title,
      artist,
      albumArt: fillResAlbumArt,
      isPlaying,
    };
  },
  togglePlayback: () => {
    const npb = document.querySelector("aside");
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };
    npb.querySelector("button[data-testid='control-button-playpause']").click();
    return { success: true, isPlaying: getIsPlaying(npb) };
  },
  skipTrack: () => {
    const npb = document.querySelector("aside");
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };
    npb.querySelector("button[data-testid='control-button-skip-forward']").click();
    return { success: true };
  },
  previousTrack: () => {
    const npb = document.querySelector("aside");
    if (!npb?.querySelector) return { success: false, error: "Now playing bar not found" };
    npb.querySelector("button[data-testid='control-button-skip-back']").click();
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

// Periodically poll for playback state
setInterval(() => {
  const state = handlers.getPlaybackState();
  ipc.postMessage({ type: "playbackStateUpdate", data: state });
}, 1000);
