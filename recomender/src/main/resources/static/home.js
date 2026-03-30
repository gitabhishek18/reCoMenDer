async function initHomePage() {
  await renderNavbar();

  const heroTitle = document.getElementById("heroTitle");
  const heroText = document.getElementById("heroText");
  const heroActions = document.getElementById("heroActions");

  const profile = await fetchCurrentProfile();

  if (profile) {
    heroTitle.textContent = `Welcome back, ${profile.name || "friend"}.`;
    heroText.textContent =
      "Your taste profile is alive. Search something new, revisit your ratings, or let the recommender suggest your next watch.";

    heroActions.innerHTML = `
      <a class="primary-btn" href="search.html">Explore Movies</a>
      <a class="soft-btn" href="recommendations.html">See Recommendations</a>
    `;
  } else {
    heroTitle.textContent = "Find movies, shape your taste, and let the backend do the heavy lifting.";
    heroText.textContent =
      "Explore movies publicly, then log in when you want ratings, history, and personalized recommendations.";

    heroActions.innerHTML = `
      <a class="primary-btn" href="search.html">Explore Movies</a>
      <a class="ghost-btn" href="login.html">Login</a>
    `;
  }
}