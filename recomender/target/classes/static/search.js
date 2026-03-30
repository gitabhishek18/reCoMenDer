let searchDebounceTimer = null;
let suggestionAbortController = null;

async function initSearchPage() {
  await renderNavbar();
  document.body.style.visibility = "visible";

  const sideNote = document.getElementById("searchSideNote");
  sideNote.innerHTML = isLoggedIn()
    ? `You’re signed in, so rating controls are live on each movie card.`
    : `You can explore freely here. Login only when you want to rate movies and shape recommendations.`;

  const input = document.getElementById("searchQuery");
  input.addEventListener("input", handleSuggestionInput);
  input.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      runSearch();
    }
  });

  document.addEventListener("click", (e) => {
    const wrapper = document.querySelector(".search-wrapper");
    if (wrapper && !wrapper.contains(e.target)) {
      hideSuggestions();
    }
  });
}

function hideSuggestions() {
  const box = document.getElementById("suggestions");
  box.style.display = "none";
  box.innerHTML = "";
}

async function handleSuggestionInput() {
  const query = document.getElementById("searchQuery").value.trim();
  const suggestions = document.getElementById("suggestions");

  clearTimeout(searchDebounceTimer);

  if (query.length < 2) {
    if (suggestionAbortController) suggestionAbortController.abort();
    hideSuggestions();
    return;
  }

  searchDebounceTimer = setTimeout(async () => {
    try {
      if (suggestionAbortController) {
        suggestionAbortController.abort();
      }

      suggestionAbortController = new AbortController();

      const response = await fetch(
        `${API_BASE}/movies?query=${encodeURIComponent(query)}&page=0&size=8`,
        { signal: suggestionAbortController.signal }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok || !Array.isArray(data.content) || data.content.length === 0) {
        hideSuggestions();
        return;
      }

      suggestions.innerHTML = data.content.map(movie => `
        <div class="suggestion-item" onmousedown="goToMovieFromSuggestion(${movie.id})">
          <strong>${escapeHtml(movie.title)}</strong>
          <div class="small">${escapeHtml(movie.year ?? "N/A")} • ${escapeHtml(movie.genres || "N/A")}</div>
        </div>
      `).join("");

      suggestions.style.display = "block";
    } catch (error) {
      if (error.name === "AbortError") return;
      hideSuggestions();
    }
  }, 250);
}

function goToMovieFromSuggestion(movieId) {
  hideSuggestions();
  window.location.href = `movie.html?id=${movieId}`;
}

async function runSearch() {
  const query = document.getElementById("searchQuery").value.trim();
  const results = document.getElementById("searchResults");

  if (!query) {
    showMessage("searchMessage", "Please enter a movie title.", "error");
    return;
  }

  showMessage("searchMessage", "", "");
  results.innerHTML = `<p>Searching...</p>`;

  try {
    const response = await fetch(`${API_BASE}/movies?query=${encodeURIComponent(query)}&page=0&size=20`);
    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      results.innerHTML = `<div class="message error">${formatBackendError(data, "Search failed.")}</div>`;
      return;
    }

    const movies = Array.isArray(data.content) ? data.content : [];

    if (movies.length === 0) {
      results.innerHTML = `<div class="empty-state">No matching movies found. Try a broader title or another spelling.</div>`;
      return;
    }

    results.innerHTML = movies.map(movie => renderMovieCard(movie)).join("");
  } catch {
    results.innerHTML = `<div class="message error">Server error while searching movies.</div>`;
  }
}

function ratingSelectHtml(movieId) {
  return `
    <select id="rating-select-${movieId}">
      <option value="">Select rating</option>
      <option value="0.5">0.5</option>
      <option value="1.0">1.0</option>
      <option value="1.5">1.5</option>
      <option value="2.0">2.0</option>
      <option value="2.5">2.5</option>
      <option value="3.0">3.0</option>
      <option value="3.5">3.5</option>
      <option value="4.0">4.0</option>
      <option value="4.5">4.5</option>
      <option value="5.0">5.0</option>
    </select>
  `;
}

function renderMovieCard(movie) {
  const movieId = normalizeMovieId(movie);
  const loggedIn = isLoggedIn();

  return `
    <div class="movie-card">
      <h3>
        <a href="movie.html?id=${movieId}" class="movie-title-link">
          ${escapeHtml(movie.title)}
        </a>
      </h3>

      <div class="meta-line">
        <span class="badge">Movie ID ${escapeHtml(movieId)}</span>
        <span class="badge">${escapeHtml(movie.year ?? "Year N/A")}</span>
      </div>

      <p><strong>Genres:</strong> ${escapeHtml(movie.genres || "N/A")}</p>

      ${
        loggedIn
          ? `
            <div class="grid-2" style="margin-top: 14px;">
              <div>
                <label for="rating-select-${movieId}">Your rating</label>
                ${ratingSelectHtml(movieId)}
              </div>
              <div style="display:flex; align-items:end;">
                <button class="primary-btn" type="button" onclick="rateMovieFromSearch(${movieId})">
                  Rate this movie
                </button>
              </div>
            </div>
            <div id="rate-msg-${movieId}"></div>
          `
          : `
            <div class="preview-lock">
              Want to rate this movie?
              <a href="login.html" style="color:var(--primary); font-weight:700;">Login</a>
              and your choices will shape recommendations.
            </div>
          `
      }

      <div class="card-actions">
        <a class="soft-btn" href="movie.html?id=${movieId}">Open details</a>
      </div>
    </div>
  `;
}

async function rateMovieFromSearch(movieId) {
  if (!isLoggedIn()) {
    window.location.replace("login.html");
    return;
  }

  const select = document.getElementById(`rating-select-${movieId}`);
  const msg = document.getElementById(`rate-msg-${movieId}`);
  const rating = select.value;

  if (!rating) {
    msg.innerHTML = `<div class="message error">Please select a rating.</div>`;
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/me/ratings`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({
        movieId: Number(movieId),
        rating: Number(rating)
      })
    });

    const data = await response.json().catch(() => ({}));

    if (response.status === 401 || response.status === 403) {
      clearAuthState();
      window.location.replace("login.html");
      return;
    }

    if (!response.ok) {
      msg.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to submit rating.")}</div>`;
      return;
    }

    msg.innerHTML = `<div class="message success">Nice. Your rating has been saved.</div>`;
  } catch {
    msg.innerHTML = `<div class="message error">Server error while submitting rating.</div>`;
  }
}