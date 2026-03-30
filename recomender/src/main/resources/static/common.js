const API_BASE = "";
let currentProfileCache = null;

function getToken() {
  return localStorage.getItem("token");
}

function decodeJwtPayload(token) {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    return JSON.parse(atob(parts[1]));
  } catch {
    return null;
  }
}

function isTokenExpired(token) {
  if (!token) return true;
  const payload = decodeJwtPayload(token);
  if (!payload || !payload.exp) return false; // if exp missing, don't hard-fail
  return Date.now() / 1000 > payload.exp;
}

function clearAuthState() {
  localStorage.removeItem("token");
  currentProfileCache = null;
}

function getValidToken() {
  const token = getToken();
  if (!token) return null;

  if (isTokenExpired(token)) {
    clearAuthState();
    return null;
  }

  return token;
}

function isLoggedIn() {
  return !!getValidToken();
}

function logout() {
  clearAuthState();
  window.location.replace("index.html");
}

function authHeaders() {
  const token = getValidToken();
  return {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${token}`
  };
}

function ensureLoggedIn() {
  const token = getValidToken();

  if (!token) {
    clearAuthState();
    window.location.replace("login.html");
    return false;
  }

  return true;
}

function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatBackendError(data, fallbackMessage) {
  if (!data) return fallbackMessage;
  if (typeof data === "string") return data;
  if (data.error && typeof data.error === "string") return data.error;
  if (data.message && typeof data.message === "string") return data.message;

  if (data.details && typeof data.details === "object") {
    return Object.entries(data.details)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join("<br>");
  }

  if (data.fields && typeof data.fields === "object") {
    return Object.entries(data.fields)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join("<br>");
  }

  return fallbackMessage;
}

function normalizeMovieId(movie) {
  return movie.movieId ?? movie.id ?? null;
}

function getAvatarLetter(nameOrEmail) {
  if (!nameOrEmail) return "U";
  return String(nameOrEmail).trim().charAt(0).toUpperCase() || "U";
}

function normalizeTimestamp(ts) {
  if (ts === null || ts === undefined || ts === "") return null;

  const num = Number(ts);
  if (Number.isNaN(num)) return null;

  if (num < 1000000000000) {
    return num * 1000;
  }

  return num;
}

function formatTimestamp(ts) {
  const normalized = normalizeTimestamp(ts);
  if (!normalized) return "N/A";
  return new Date(normalized).toLocaleString();
}

function showMessage(elementId, message, type) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.innerHTML = message ? `<div class="message ${type}">${message}</div>` : "";
}

function togglePassword(inputId, buttonId) {
  const input = document.getElementById(inputId);
  const button = document.getElementById(buttonId);
  if (!input || !button) return;

  if (input.type === "password") {
    input.type = "text";
    button.textContent = "Hide";
  } else {
    input.type = "password";
    button.textContent = "Show";
  }
}

async function fetchCurrentProfile() {
  if (!isLoggedIn()) return null;
  if (currentProfileCache) return currentProfileCache;

  try {
    const response = await fetch(`${API_BASE}/me/profile`, {
      method: "GET",
      headers: authHeaders()
    });

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        clearAuthState();
      }
      return null;
    }

    const data = await response.json().catch(() => null);
    currentProfileCache = data;
    return data;
  } catch {
    return null;
  }
}

function navbarHtml(profile = null) {
  const loggedIn = isLoggedIn();
  const currentPath = window.location.pathname.split("/").pop() || "index.html";
  const avatarLabel = getAvatarLetter(profile?.name || profile?.email);

  const navLink = (href, label) => `
    <a class="nav-link ${currentPath === href ? "active" : ""}" href="${href}">${label}</a>
  `;

  return `
    <div class="navbar">
      <div class="nav-inner">
        <div class="nav-left">
          <a class="brand" href="index.html">MovieRec</a>
          <div class="nav-links">
            ${navLink("index.html", "Home")}
            ${navLink("search.html", "Search")}
            ${loggedIn ? navLink("recommendations.html", "Recommendations") : ""}
          </div>
        </div>

        <div class="nav-right">
          ${
            loggedIn
              ? `
                <div class="avatar-wrap">
                  <button class="avatar-btn" id="avatarToggleBtn" type="button">${escapeHtml(avatarLabel)}</button>
                  <div class="avatar-menu" id="avatarMenu">
                    <div class="avatar-user">
                      <strong>${escapeHtml(profile?.name || "User")}</strong>
                      <span>${escapeHtml(profile?.email || "")}</span>
                    </div>
                    <a class="menu-link" href="profile.html">My Profile</a>
                    <a class="menu-link" href="ratings.html">My Ratings</a>
                    <a class="menu-link" href="recommendations.html">Recommendations</a>
                    <button class="menu-btn" type="button" onclick="logout()">Logout</button>
                  </div>
                </div>
              `
              : `
                <a class="ghost-btn" href="login.html">Login</a>
              `
          }
        </div>
      </div>
    </div>
  `;
}

async function renderNavbar() {
  const navRoot = document.getElementById("navbarRoot");
  if (!navRoot) return;

  const profile = await fetchCurrentProfile();
  navRoot.innerHTML = navbarHtml(profile);

  const btn = document.getElementById("avatarToggleBtn");
  const menu = document.getElementById("avatarMenu");

  if (btn && menu) {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      menu.classList.toggle("open");
    });

    document.addEventListener("click", (e) => {
      const wrap = document.querySelector(".avatar-wrap");
      if (wrap && !wrap.contains(e.target)) {
        menu.classList.remove("open");
      }
    });
  }
}

window.addEventListener("pageshow", async () => {
  if (!getValidToken()) {
    clearAuthState();
  }

  const navRoot = document.getElementById("navbarRoot");
  if (navRoot) {
    await renderNavbar();
  }

  const protectedPages = ["profile.html", "ratings.html", "recommendations.html"];
  const currentPage = window.location.pathname.split("/").pop();

  if (protectedPages.includes(currentPage) && !getValidToken()) {
    window.location.replace("login.html");
  }
});