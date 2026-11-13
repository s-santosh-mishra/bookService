(function () {
  const isLocal =
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1" ||
    window.location.hostname === "";

  const API_BASE_URL = isLocal
    ? "http://localhost:8080"
    : "https://bookservice-z2kr.onrender.com";

  function isDateOnly(str) {
    return /^\d{4}-\d{2}-\d{2}$/.test(str);
  }

  function parseUtcToDate(value) {
    if (value === null || value === undefined || value === "") return null;
    if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;
    if (typeof value === "number") {
      const d = new Date(value);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    let str = String(value).trim();
    if (!str) return null;
    if (isDateOnly(str)) return null;

    const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
    if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
      str = str.replace(" ", "T") + "Z";
    }
    const date = new Date(str);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  function parseIstToDate(value) {
    if (value === null || value === undefined || value === "") return null;
    if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;
    if (typeof value === "number") {
      const d = new Date(value);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    let str = String(value).trim();
    if (!str) return null;
    if (isDateOnly(str)) return null;

    const hasOffset = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(str);
    if (!hasOffset && /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(str)) {
      str = str.replace(" ", "T") + "+05:30";
    }
    const date = new Date(str);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  function formatCalendarDate(value, options) {
    const fallback = options && options.fallback !== undefined ? options.fallback : "—";
    if (value === null || value === undefined || value === "") return fallback;
    const str = String(value).trim();
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(str);
    if (!match) return fallback;
    const year = Number(match[1]);
    const month = Number(match[2]) - 1;
    const day = Number(match[3]);
    const date = new Date(Date.UTC(year, month, day));
    const opts = Object.assign(
      {
        timeZone: "UTC",
        day: "2-digit",
        month: "short",
        year: "numeric"
      },
      options
    );
    delete opts.fallback;
    return new Intl.DateTimeFormat("en-IN", opts).format(date);
  }

  function formatDateTime(value, options) {
    const date = parseUtcToDate(value);
    const fallback = options && options.fallback !== undefined ? options.fallback : "—";
    if (!date) return fallback;
    const opts = Object.assign(
      {
        timeZone: "Asia/Kolkata",
        dateStyle: "medium",
        timeStyle: "short"
      },
      options
    );
    delete opts.fallback;
    return new Intl.DateTimeFormat("en-IN", opts).format(date);
  }

  function formatIstDateTime(value, options) {
    const date = parseIstToDate(value);
    const fallback = options && options.fallback !== undefined ? options.fallback : "—";
    if (!date) return fallback;
    const opts = Object.assign(
      {
        timeZone: "Asia/Kolkata",
        dateStyle: "medium",
        timeStyle: "short"
      },
      options
    );
    delete opts.fallback;
    return new Intl.DateTimeFormat("en-IN", opts).format(date);
  }

  function formatDate(value, options) {
    if (typeof value === "string" && isDateOnly(value.trim())) {
      return formatCalendarDate(value, options);
    }
    const date = parseUtcToDate(value);
    const fallback = options && options.fallback !== undefined ? options.fallback : "—";
    if (!date) return fallback;
    const opts = Object.assign(
      {
        timeZone: "Asia/Kolkata",
        dateStyle: "medium"
      },
      options
    );
    delete opts.fallback;
    return new Intl.DateTimeFormat("en-IN", opts).format(date);
  }

  function formatTime(value, options) {
    const date = parseUtcToDate(value);
    const fallback = options && options.fallback !== undefined ? options.fallback : "—";
    if (!date) return fallback;
    const opts = Object.assign(
      {
        timeZone: "Asia/Kolkata",
        timeStyle: "short"
      },
      options
    );
    delete opts.fallback;
    return new Intl.DateTimeFormat("en-IN", opts).format(date);
  }

  function formatDateTimeParts(value) {
    const date = parseUtcToDate(value);
    if (!date) {
      return {
        date: "Not available",
        time: "Not available"
      };
    }
    const dateStr = new Intl.DateTimeFormat("en-IN", {
      timeZone: "Asia/Kolkata",
      day: "2-digit",
      month: "short",
      year: "numeric"
    }).format(date);
    const timeStr = new Intl.DateTimeFormat("en-IN", {
      timeZone: "Asia/Kolkata",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true
    }).format(date);
    return {
      date: dateStr,
      time: timeStr
    };
  }

  function getLocalDateValue(value) {
    if (typeof value === "string" && isDateOnly(value.trim())) {
      return value.trim();
    }
    const date = parseUtcToDate(value);
    if (!date) return "";
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone: "Asia/Kolkata",
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    }).formatToParts(date);
    const y = parts.find((p) => p.type === "year")?.value;
    const m = parts.find((p) => p.type === "month")?.value;
    const d = parts.find((p) => p.type === "day")?.value;
    return `${y}-${m}-${d}`;
  }

  function getTimestamp(value) {
    const date = parseUtcToDate(value);
    return date ? date.getTime() : 0;
  }

  function getIstTimestamp(value) {
    const date = parseIstToDate(value);
    return date ? date.getTime() : 0;
  }

  const SERVICEHUB_DATE = {
    isDateOnly: isDateOnly,
    parseUtcToDate: parseUtcToDate,
    parseIstToDate: parseIstToDate,
    formatDateTime: formatDateTime,
    formatIstDateTime: formatIstDateTime,
    formatCalendarDate: formatCalendarDate,
    formatDate: formatDate,
    formatTime: formatTime,
    formatDateTimeParts: formatDateTimeParts,
    getLocalDateValue: getLocalDateValue,
    getTimestamp: getTimestamp,
    getIstTimestamp: getIstTimestamp
  };

  // Inject CSS for consistent, dependency-free spinner animation
  if (typeof document !== "undefined" && !document.getElementById("servicehub-spinner-style")) {
    const style = document.createElement("style");
    style.id = "servicehub-spinner-style";
    style.textContent = `
      @keyframes servicehub-spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
      }
      .servicehub-spinner {
        animation: servicehub-spin 0.75s linear infinite !important;
        display: inline-block;
        vertical-align: middle;
      }
    `;
    document.head.appendChild(style);
  }

  function createSpinnerSvg() {
    return `
      <svg class="servicehub-spinner h-4 w-4 shrink-0 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    `.trim();
  }

  function setButtonLoading(button, loadingText, options = {}) {
    if (!button || !(button instanceof Element)) return null;

    // Prevent duplicate triggers if already loading
    if (button.dataset.servicehubLoading === "true") {
      return null;
    }

    const originalHtml = button.innerHTML;
    const originalDisabled = button.disabled;
    const originalAriaBusy = button.getAttribute("aria-busy");
    const originalMinWidth = button.style.minWidth;

    // Dimension stability: preserve current rendered width if not already fixed
    if (options.lockWidth !== false) {
      const rect = button.getBoundingClientRect();
      if (rect.width > 0 && !button.style.minWidth) {
        button.style.minWidth = `${Math.ceil(rect.width)}px`;
      }
    }

    button.dataset.servicehubLoading = "true";
    button.disabled = true;
    button.setAttribute("aria-busy", "true");

    const text = loadingText || "Processing...";
    const spinner = createSpinnerSvg();

    button.innerHTML = `
      <span class="inline-flex items-center justify-center gap-2 pointer-events-none w-full">
        ${spinner}
        <span>${text}</span>
      </span>
    `.trim();

    let restored = false;
    const restore = function () {
      if (restored) return;
      restored = true;
      button.innerHTML = originalHtml;
      button.disabled = originalDisabled;
      button.style.minWidth = originalMinWidth;
      if (originalAriaBusy !== null) {
        button.setAttribute("aria-busy", originalAriaBusy);
      } else {
        button.removeAttribute("aria-busy");
      }
      delete button.dataset.servicehubLoading;
      delete button._servicehubRestore;
    };

    button._servicehubRestore = restore;
    return restore;
  }

  function restoreButton(button) {
    if (button && typeof button._servicehubRestore === "function") {
      button._servicehubRestore();
    }
  }

  async function withButtonLoading(button, loadingText, actionFn) {
    const restore = setButtonLoading(button, loadingText);
    if (!restore) return null;
    try {
      return await actionFn();
    } finally {
      restore();
    }
  }

  const SERVICEHUB_BUTTON = {
    setLoading: setButtonLoading,
    restore: restoreButton,
    withLoading: withButtonLoading
  };

  window.SERVICEHUB_BUTTON = SERVICEHUB_BUTTON;
  window.SERVICEHUB_DATE = SERVICEHUB_DATE;

  window.SERVICEHUB_CONFIG = {
    API_BASE_URL: API_BASE_URL,
    isLocal: isLocal,
    dateUtils: SERVICEHUB_DATE,
    buttonUtils: SERVICEHUB_BUTTON
  };
})();
