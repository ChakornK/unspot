const originalCreateElement = document.createElement;
let oldVolume = null;
document.createElement = (() => {
  return function () {
    const element = originalCreateElement.apply(this, arguments);
    if (element instanceof HTMLMediaElement) {
      element.addEventListener("play", (event) => {
        if (!event.currentTarget.src.startsWith("blob:https://open.spotify.com/") && event.currentTarget.duration < 40) {
          const target = event.currentTarget;
          setTimeout(() => {
            if (target.volume !== 0.0001) {
              oldVolume = target.volume;
              target.volume = 0.0001;
            }
            target.currentTime = target.duration;
          }, 1);
        } else {
          if (oldVolume !== null) {
            event.currentTarget.volume = oldVolume;
            oldVolume = null;
          }
        }
      });
      element.addEventListener("ended", (event) => {
        if (!event.currentTarget.src.startsWith("blob:https://open.spotify.com/") && event.currentTarget.duration < 40) {
          setTimeout(() => {
            try {
              event.currentTarget.play();
            } catch {}
          }, 50);
        }
      });
    }
    return element;
  };
})();
