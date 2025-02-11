// Function to handle login
async function handleLogin(event) {
    event.preventDefault(); // Prevent the form from submitting normally

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    if (!username || !password) {
        showError("Please enter both username and password.");
        return;
    }
    console.log(password);

    try {
        const response = await fetch("/user/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                username: username,
                plainPassword: password,
            }),
        });

        if (response.ok) {
            const data = await response.json();
            const jwt = data.token;

            if (jwt) {
                localStorage.setItem("jwt", jwt); // Store the JWT in localStorage
                window.location.href = "/chat/chats.html"; // Redirect to the chats page
            } else {
                showError("Login failed: No JWT received.");
            }
        } else {
            const error = await response.json();
            showError(error.message || "Invalid username or password.");
        }
    } catch (err) {
        showError("Unable to connect to the server.");
    }
}

// Function to show error messages
function showError(message) {
    const errorMessage = document.getElementById("error-message");
    errorMessage.textContent = message;
    errorMessage.style.color = "red";
    errorMessage.style.display = "block";
}

// Attach event listener to the login button
document.getElementById("login-form").addEventListener("submit", handleLogin);
