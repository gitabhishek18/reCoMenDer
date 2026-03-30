async function initRecommendationsPage() {
  if (!ensureLoggedIn()) return;
  await renderNavbar();
  document.body.style.visibility = "visible";
}

async function loadRecommendations() {
  const strategy = document.getElementById("strategy").value;
  const k = document.getElementById("kValue").value.trim();
  const container = document.getElementById("recommendationsContainer");

  const num = parseInt(k, 10);
  if (!num || num < 1 || num > 50) {
    container.innerHTML = `<div class="message error">Please enter a valid k between 1 and 50.</div>`;
    return;
  }

  container.innerHTML = `<p>Loading recommendations...</p>`;

  try {
    const response = await fetch(`${API_BASE}/me/recommendations?strategy=${encodeURIComponent(strategy)}&k=${encodeURIComponent(num)}`, {
      method: "GET",
      headers: authHeaders()
    });

    if (response.status === 401 || response.status === 403) {
      clearAuthState();
      window.location.replace("login.html");
      return;
    }

    const data = await response.json().catch(() => []);

    if (!response.ok) {
      container.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to load recommendations.")}</div>`;
      return;
    }

    if (!Array.isArray(data) || data.length === 0) {
      container.innerHTML = `<div class="empty-state">No recommendations found for this request.</div>`;
      return;
    }

    container.innerHTML = data.map(item => `
      <div class="rec-card">
        <h3>
          <a href="movie.html?id=${item.movieId}" class="movie-title-link">
            ${escapeHtml(item.title)}
          </a>
        </h3>

        <div class="meta-line">
          <span class="badge">Movie ID ${escapeHtml(item.movieId)}</span>
          <span class="badge">Score ${escapeHtml(item.score)}</span>
          <span class="badge">${escapeHtml(strategy.toUpperCase())}</span>
        </div>

        <div class="subtle-box">
          <strong>Why this one:</strong> ${escapeHtml(item.reason || "No explanation available.")}
        </div>
      </div>
    `).join("");
  } catch {
    container.innerHTML = `<div class="message error">Server error while loading recommendations.</div>`;
  }
}