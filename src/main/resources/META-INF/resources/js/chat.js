// Get chatId from the URL
const chatId = window.location.pathname.split("/").pop();
const messageBox = document.getElementById("message-box");
const messageInput = document.getElementById("message-input");
const sendBtn = document.getElementById("send-btn");
let socket;

// Get the JWT token (you need to replace this logic with your actual token retrieval mechanism)
const token = localStorage.getItem("jwt"); // Assuming it's stored in localStorage

if (!token) {
    alert("You are not logged in. Please log in to access the chat.");
    window.location.href = ""; // Redirect to login page
}

// Load chat messages on page load
window.addEventListener("DOMContentLoaded", async () => {
    await loadMessages();
    setupWebSocket();
});

// Function to load messages
async function loadMessages(page = 0) {
    try {
        const response = await fetch(`/chat/messages?chatId=${chatId}&page=${page}`, {
            headers: {
                Authorization: `Bearer ${token}`, // Include the Bearer token
            },
        });
        if (response.ok) {
            const messages = await response.json();
            displayMessages(messages);
        } else {
            console.error("Failed to load messages.");
        }
    } catch (err) {
        console.error("Error loading messages:", err);
    }
}

// Function to display messages
function displayMessages(messages) {
    messageBox.innerHTML = ""; // Clear current messages
    messages.forEach(({ message, fromUsername, timestamp }) => {
        const messageDiv = document.createElement("div");
        messageDiv.classList.add("message");
        messageDiv.classList.add(fromUsername === "You" ? "sent" : "received");

        messageDiv.innerHTML = `
            <p>${message}</p>
            <p class="timestamp">${fromUsername} - ${new Date(timestamp).toLocaleString()}</p>
        `;
        messageBox.appendChild(messageDiv);
    });

    // Scroll to the bottom
    messageBox.scrollTop = messageBox.scrollHeight;
}

// Setup WebSocket for live messages
function setupWebSocket() {
    socket = new WebSocket(`ws://localhost:8080/chat/ws/${chatId}?token=${token}`); // Include token as a query parameter

    // Handle incoming messages
    socket.onmessage = (event) => {
        const { message, fromUsername, timestamp } = JSON.parse(event.data);

        const messageDiv = document.createElement("div");
        messageDiv.classList.add("message");
        messageDiv.classList.add(fromUsername === "You" ? "sent" : "received");

        messageDiv.innerHTML = `
            <p>${message}</p>
            <p class="timestamp">${fromUsername} - ${new Date(timestamp).toLocaleString()}</p>
        `;
        messageBox.appendChild(messageDiv);

        // Scroll to the bottom
        messageBox.scrollTop = messageBox.scrollHeight;
    };

    // Handle errors
    socket.onerror = (error) => console.error("WebSocket error:", error);

    // Handle connection close
    socket.onclose = () => console.log("WebSocket connection closed.");
}

// Function to send a message
sendBtn.addEventListener("click", () => {
    const message = messageInput.value.trim();
    if (message) {
        socket.send(message); // Send message to WebSocket
        messageInput.value = ""; // Clear input
    }
});
