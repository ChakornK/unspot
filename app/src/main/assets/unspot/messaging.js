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

const getLibraryData = () => {
  return new Promise((resolve) => {
    const scrollContainer = document.querySelector("nav .YourLibraryX [data-overlayscrollbars-viewport]");
    const listContainer = document.querySelector("nav .YourLibraryX [role='presentation']:not([data-testid])");
    const libraryItems = new Map();

    const parseItems = () => {
      const rows = document.querySelectorAll("nav .YourLibraryX [role='presentation']:not([data-testid]) > [aria-rowindex]");
      for (const p of rows) {
        const index = p.getAttribute("aria-rowindex");
        if (libraryItems.has(index)) continue;

        const type = p
          .querySelector("[aria-labelledby*='listrow-title']")
          ?.getAttribute("aria-labelledby")
          ?.match(/(?<=listrow-title-spotify:).+?(?=:)/i)?.[0];
        const cover = p.querySelector("img")?.getAttribute("src");
        const title = p.querySelector("[data-encore-id='listRowTitle']")?.textContent;
        const subtitle = p.querySelector("[data-encore-id='listRowSubtitle']")?.textContent;
        const isActive = !!p.querySelector("[data-encore-id='listRowTitle'][class*='accent']");

        if (!type || !cover || !title || !subtitle) continue;

        libraryItems.set(index, {
          index,
          type,
          cover,
          title,
          subtitle,
          isActive,
        });
      }
    };

    const scrollToNextWindow = () => {
      const last = document.querySelector("nav .YourLibraryX [role='presentation']:not([data-testid]) > [aria-rowindex]:last-child");
      last?.scrollIntoView({ behavior: "instant", block: "start" });
    };

    let endTimer = null;
    const checkListEnd = () => {
      clearTimeout(endTimer);
      endTimer = setTimeout(() => {
        observer.disconnect();
        resolve({
          items: [...libraryItems.values()],
        });
      }, 300);
    };

    const observer = new MutationObserver(() => {
      clearTimeout(endTimer);
      parseItems();
      scrollToNextWindow();
      checkListEnd();
    });
    observer.observe(listContainer, {
      childList: true,
      subtree: true,
    });

    scrollContainer.addEventListener(
      "scrollend",
      () => {
        parseItems();
        scrollToNextWindow();
        checkListEnd();
      },
      { once: true },
    );
    scrollContainer.scrollTo({ behavior: "instant", top: 0 });
  });
};

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

  getLibraryData: () => {
    return getLibraryData();
  },
};

ipc.onMessage.addListener(async (message) => {
  console.log("Received message from app:", message);
  const handler = handlers[message.type];
  if (handler) {
    try {
      const result = await handler(message.data);
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

const postLibraryUpdate = async () => {
  ipc.postMessage({ type: "getLibraryDataResponse", data: await getLibraryData() });
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
