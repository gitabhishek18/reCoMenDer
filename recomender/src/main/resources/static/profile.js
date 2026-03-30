async function initProfilePage() {
  if (!ensureLoggedIn()) return;
  await renderNavbar();
  document.body.style.visibility = "visible";
  loadProfile();
}

async function loadProfile() {
  const container = document.getElementById("profileContainer");

  try {
    const response = await fetch(`${API_BASE}/me/profile`, {
      method: "GET",
      headers: authHeaders()
    });

    if (response.status === 401 || response.status === 403) {
      clearAuthState();
      window.location.replace("login.html");
      return;
    }

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      container.innerHTML = `<div class="message error">${formatBackendError(data, "Failed to load profile.")}</div>`;
      return;
    }

    container.innerHTML = `
      <div class="profile-grid">
        <div class="detail-card">
          <div class="profile-summary">
            <div class="profile-avatar-large">${escapeHtml(getAvatarLetter(data.name || data.email))}</div>
            <div>
              <div class="kicker">Profile</div>
              <h2 style="margin-bottom:6px;">${escapeHtml(data.name || "User")}</h2>
              <p class="muted">This is the account currently authenticated through JWT.</p>
            </div>
          </div>
        </div>

        <div class="detail-card">
          <div class="kicker">Quick facts</div>
          <div class="info-list">
            <p><strong>User ID:</strong> ${escapeHtml(data.userId)}</p>
            <p><strong>Email:</strong> ${escapeHtml(data.email)}</p>
            <p><strong>Role:</strong> ${escapeHtml(data.role)}</p>
          </div>
        </div>
      </div>
    `;
  } catch {
    container.innerHTML = `<div class="message error">Server error while loading profile.</div>`;
  }
}