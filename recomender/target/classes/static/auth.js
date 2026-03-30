async function redirectIfAlreadyLoggedIn() {
  if (isLoggedIn()) {
    window.location.replace("index.html");
    return;
  }
  document.body.style.visibility = "visible";
}

async function register() {
  const name = document.getElementById("registerName").value.trim();
  const email = document.getElementById("registerEmail").value.trim();
  const password = document.getElementById("registerPassword").value.trim();

  if (!name || !email || !password) {
    showMessage("registerMessage", "Please fill all fields.", "error");
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ name, email, password })
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      showMessage(
        "registerMessage",
        formatBackendError(data, "Registration failed."),
        "error"
      );
      return;
    }

    if (!data.token) {
      showMessage("registerMessage", "Registration succeeded but token is missing.", "error");
      return;
    }

    localStorage.setItem("token", data.token);
    currentProfileCache = null;

    showMessage("registerMessage", "Registration successful. Redirecting...", "success");

    setTimeout(() => {
      window.location.replace("index.html");
    }, 700);

  } catch (error) {
    showMessage("registerMessage", "Server error during registration.", "error");
  }
}

async function login() {
  const email = document.getElementById("loginEmail").value.trim();
  const password = document.getElementById("loginPassword").value.trim();

  if (!email || !password) {
    showMessage("loginMessage", "Please fill email and password.", "error");
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ email, password })
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      showMessage(
        "loginMessage",
        formatBackendError(data, "Login failed."),
        "error"
      );
      return;
    }

    if (!data.token) {
      showMessage("loginMessage", "Token missing in response.", "error");
      return;
    }

    localStorage.setItem("token", data.token);
    currentProfileCache = null;

    showMessage("loginMessage", "Login successful. Redirecting...", "success");

    setTimeout(() => {
      window.location.replace("index.html");
    }, 700);

  } catch (error) {
    showMessage("loginMessage", "Server error during login.", "error");
  }
}