const ipc = browser.runtime.connectNative("browser");

ipc.onMessage.addListener((message) => {
  window.postMessage({ direction: "from-content-script", message }, "*");
});
window.addEventListener("message", (event) => {
  if (event.source === window && event?.data?.direction === "from-page-script") {
    const { message } = event.data;
    ipc.postMessage(message);
  }
});
