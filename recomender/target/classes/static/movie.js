async function initMoviePage() {
  await renderNavbar();
  document.body.style.visibility = "visible";
  await loadMovieDetails();
}

function getMovieIdFromUrl() {
  const params = new URLSearchParams(window.location.search);
  return params.get("id");
}

function ratingSelectHtml(movieId) {
  return `
    <select id="detail-rating-select-${movieId}">
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

async function loadMovieDetails() {
  const movieId = getMovieIdFromUrl();
  const container = document.getElementById("movieDetailContainer");

  if (!movieId) {
    container.innerHTML = `<div class="message error">Movie ID is missing.</div>`;
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/movies/${encodeURIComponent(movieId)}`);
    const movie = await response.json().catch(() => ({}));

    if (!response.ok) {
      container.innerHTML = `<div class="message error">${formatBackendError(movie, "Failed to load movie details.")}</div>`;
      return;
    }

    container.innerHTML = `
      <div class="kicker">Movie</div>
      <h2 style="font-size:32px; margin-bottom:10px;">${escapeHtml(movie.title)}</h2>
      <div class="meta-line">
        <span class="badge">Movie ID ${escapeHtml(movie.id)}</span>
        <span class="badge">${escapeHtml(movie.year ?? "Year N/A")}</span>
      </div>
      <p style="line-height:1.75;"><strong>Genres:</strong> ${escapeHtml(movie.genres || "N/A")}</p>

      ${
        isLoggedIn()
          ? `
            <div class="card" style="margin-top:18px; margin-bottom:0;">
              <h3 style="margin-bottom:10px;">Rate this movie</h3>
              <div class="inline-row">
                <div>
                  <label for="detail-rating-select-${movie.id}">Your rating</label>
                  ${ratingSelectHtml(movie.id)}
                </div>
                <div class="fit">
                  <button class="primary-btn" onclick="rateMovieFromDetail(${movie.id})">Save rating</button>
                </div>
              </div>
              <div id="detail-rate-msg"></div>
            </div>
          `
          : `
            <div class="preview-lock" style="margin-top:18px;">
              You can explore details publicly.
              <a href="login.html" style="color:var(--primary); font-weight:700;">Login</a>
              when you want to rate this movie.
            </div>
          `
      }
    `;
  } catch {
    container.innerHTML = `<div class="message error">Server error while loading movie details.</div>`;
  }
}

async function rateMovieFromDetail(movieId) {
  const select = document.getElementById(`detail-rating-select-${movieId}`);
  const msg = document.getElementById("detail-rate-msg");
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

    msg.innerHTML = `<div class="message success">Your rating is saved.</div>`;
  } catch {
    msg.innerHTML = `<div class="message error">Server error while saving rating.</div>`;
  }
}