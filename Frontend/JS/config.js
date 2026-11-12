(function () {
  const isLocal =
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1" ||
    window.location.hostname === "";

  const API_BASE_URL = isLocal
    ? "http://localhost:8080"
    : "https://bookservice-z2kr.onrender.com";

  window.SERVICEHUB_CONFIG = {
    API_BASE_URL: API_BASE_URL,
    isLocal: isLocal
  };
})();

