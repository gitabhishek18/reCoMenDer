let currentRatingsData = [];

async function initRatingsPage() {
  if (!ensureLoggedIn()) return;
  await renderNavbar();
  document.body.style.visibility = "visible";
  loadRatings();
}

async function loadRatings() {
  const container = document.getElementById("ratingsContainer");

  try {
    const response = await fetch(`${API_BASE}/me/ratings`, {
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
      container.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to load ratings.")}</div>`;
      return;
    }

    currentRatingsData = Array.isArray(data) ? data.slice() : [];
    renderRatingsList(currentRatingsData);
  } catch {
    container.innerHTML = `<div class="message error">Server error while loading ratings.</div>`;
  }
}

function renderRatingsList(data) {
  const container = document.getElementById("ratingsContainer");

  if (!data.length) {
    container.innerHTML = `
      <div class="empty-state">
        Your rating journey hasn’t started yet. Head to search, rate a movie, and this page will begin to bloom.
      </div>
    `;
    return;
  }

  container.innerHTML = data.map(item => `
    <div class="rating-card">
      <h3>
        <a href="movie.html?id=${item.movieId}" class="movie-title-link">
          ${escapeHtml(item.title)}
        </a>
      </h3>

      <div class="meta-line">
        <span class="badge">Movie ID ${escapeHtml(item.movieId)}</span>
        <span class="badge">${escapeHtml(item.year ?? "Year N/A")}</span>
        <span class="badge">Rated ${escapeHtml(item.rating)}</span>
      </div>

      <p><strong>Genres:</strong> ${escapeHtml(item.genres || "N/A")}</p>
      <p class="small" style="margin-top:10px;">
        <strong>Rated At:</strong> ${formatTimestamp(item.ratedAt)}
      </p>
    </div>
  `).join("");
}