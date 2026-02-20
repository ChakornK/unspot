const originalCreateElement = document.createElement;
let oldVolume = null;
document.createElement = (() => {
  return function () {
    const element = originalCreateElement.apply(this, arguments);
    if (element instanceof HTMLMediaElement) {
      const oldPlay = element.play;
      element.play = function () {
        if (!this.src.startsWith("blob:https://open.spotify.com/") && this.duration < 40) {
          window.adblockDebug = this;
          this.dispatchEvent(new Event("play"));
          setTimeout(() => {
            this.currentTime = this.duration;
            this.dispatchEvent(new Event("timeupdate"));
            const oldSrc = this.src;
            const inter = setInterval(() => {
              if (this.src !== oldSrc) return clearInterval(inter);
              this.currentTime = this.duration;
              this.dispatchEvent(new Event("ended"));
            }, 20);
            setTimeout(() => {
              try {
                clearInterval(inter);
              } catch {}
            }, 10000);
          }, 20);
        } else {
          oldPlay.apply(this, arguments);
        }
      };
    }
    return element;
  };
})();
