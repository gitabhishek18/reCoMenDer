const API_BASE = "";

function getToken() {
  return localStorage.getItem("token");
}

function logout() {
  localStorage.removeItem("token");
  window.location.href = "login.html";
}

function authHeaders() {
  return {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${getToken()}`
  };
}

function ensureLoggedIn() {
  const token = getToken();
  if (!token) {
    window.location.href = "login.html";
    return false;
  }
  return true;
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

function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function initDashboard() {
  if (!ensureLoggedIn()) return;
  await loadProfile();
  await loadMyRatings();
}

async function loadProfile() {
  const profileSection = document.getElementById("profileSection");

  try {
    const response = await fetch(`${API_BASE}/me/profile`, {
      method: "GET",
      headers: authHeaders()
    });

    if (response.status === 401 || response.status === 403) {
      logout();
      return;
    }

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      profileSection.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to load profile")}</div>`;
      return;
    }

    profileSection.innerHTML = `
      <p><strong>User ID:</strong> ${escapeHtml(data.userId)}</p>
      <p><strong>Name:</strong> ${escapeHtml(data.name)}</p>
      <p><strong>Email:</strong> ${escapeHtml(data.email)}</p>
      <p><strong>Role:</strong> ${escapeHtml(data.role)}</p>
    `;
  } catch (error) {
    profileSection.innerHTML = `<div class="message error">Server error while loading profile</div>`;
  }
}

async function loadMyRatings() {
  const myRatings = document.getElementById("myRatings");

  try {
    const response = await fetch(`${API_BASE}/me/ratings`, {
      method: "GET",
      headers: authHeaders()
    });

    if (response.status === 401 || response.status === 403) {
      logout();
      return;
    }

    const data = await response.json().catch(() => []);

    if (!response.ok) {
      myRatings.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to load ratings")}</div>`;
      return;
    }

    if (!Array.isArray(data) || data.length === 0) {
      myRatings.innerHTML = `<p>No ratings yet.</p>`;
      return;
    }

    myRatings.innerHTML = data.map(item => `
      <div class="rating-item">
        <p><strong>${escapeHtml(item.title)}</strong> (${escapeHtml(item.year ?? "N/A")})</p>
        <p><strong>Movie ID:</strong> ${escapeHtml(item.movieId)}</p>
        <p><strong>Genres:</strong> ${escapeHtml(item.genres || "N/A")}</p>
        <p><strong>Rating:</strong> ${escapeHtml(item.rating)}</p>
        <p class="small-text"><strong>Rated At:</strong> ${item.ratedAt ? new Date(item.ratedAt).toLocaleString() : "N/A"}</p>
      </div>
    `).join("");

  } catch (error) {
    myRatings.innerHTML = `<div class="message error">Server error while loading ratings</div>`;
  }
}

async function searchMovies() {
  const query = document.getElementById("searchQuery").value.trim();
  const movieResults = document.getElementById("movieResults");

  if (!query) {
    movieResults.innerHTML = `<div class="message error">Enter a search term</div>`;
    return;
  }

  movieResults.innerHTML = `<p>Searching...</p>`;

  try {
    const response = await fetch(`${API_BASE}/movies?query=${encodeURIComponent(query)}`, {
      method: "GET"
    });

    const data = await response.json().catch(() => []);

    if (!response.ok) {
      movieResults.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to search movies")}</div>`;
      return;
    }

    if (!Array.isArray(data) || data.length === 0) {
      movieResults.innerHTML = `<p>No movies found.</p>`;
      return;
    }

    movieResults.innerHTML = data.map(movie => {
      const movieId = normalizeMovieId(movie);
      return `
        <div class="movie-item">
          <p><strong>${escapeHtml(movie.title)}</strong> (${escapeHtml(movie.year ?? "N/A")})</p>
          <p><strong>Movie ID:</strong> ${escapeHtml(movieId)}</p>
          <p><strong>Genres:</strong> ${escapeHtml(movie.genres || "N/A")}</p>
          <button onclick="fillMovieId(${movieId})">Rate This Movie</button>
        </div>
      `;
    }).join("");

  } catch (error) {
    movieResults.innerHTML = `<div class="message error">Server error while searching movies</div>`;
  }
}

function fillMovieId(movieId) {
  document.getElementById("rateMovieId").value = movieId;
  window.scrollTo({ top: 250, behavior: "smooth" });
}

async function submitRating() {
  const movieId = document.getElementById("rateMovieId").value.trim();
  const rating = document.getElementById("rateValue").value.trim();
  const ratingMessage = document.getElementById("ratingMessage");

  if (!movieId || !rating) {
    ratingMessage.innerHTML = `<div class="message error">Please enter movie ID and rating</div>`;
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
      logout();
      return;
    }

    if (!response.ok) {
      ratingMessage.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to submit rating")}</div>`;
      return;
    }

    ratingMessage.innerHTML = `<div class="message success">Rating submitted successfully</div>`;

    document.getElementById("rateMovieId").value = "";
    document.getElementById("rateValue").value = "";

    await loadMyRatings();
    await loadRecommendations();

  } catch (error) {
    ratingMessage.innerHTML = `<div class="message error">Server error while submitting rating</div>`;
  }
}

async function loadRecommendations() {
  const strategy = document.getElementById("strategy").value;
  const k = document.getElementById("kValue").value.trim();
  const recommendations = document.getElementById("recommendations");

  if (!k) {
    recommendations.innerHTML = `<div class="message error">Please enter k value</div>`;
    return;
  }

  recommendations.innerHTML = `<p>Loading recommendations...</p>`;

  try {
    const response = await fetch(
      `${API_BASE}/me/recommendations?strategy=${encodeURIComponent(strategy)}&k=${encodeURIComponent(k)}`,
      {
        method: "GET",
        headers: authHeaders()
      }
    );

    if (response.status === 401 || response.status === 403) {
      logout();
      return;
    }

    const data = await response.json().catch(() => []);

    if (!response.ok) {
      recommendations.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to load recommendations")}</div>`;
      return;
    }

    if (!Array.isArray(data) || data.length === 0) {
      recommendations.innerHTML = `<p>No recommendations found.</p>`;
      return;
    }

    recommendations.innerHTML = data.map(item => `
      <div class="rec-item">
        <p><strong>${escapeHtml(item.title)}</strong></p>
        <p><strong>Movie ID:</strong> ${escapeHtml(item.movieId)}</p>
        <p><strong>Score:</strong> ${escapeHtml(item.score)}</p>
        <p><strong>Reason:</strong> ${escapeHtml(item.reason || "No explanation available")}</p>
      </div>
    `).join("");

  } catch (error) {
    recommendations.innerHTML = `<div class="message error">Server error while loading recommendations</div>`;
  }
}