// Function to fetch chats and display them
async function loadChats() {
    const jwt = localStorage.getItem("jwt");

    if (!jwt) {
        window.location.href = "/"; // Redirect to login if no token
        return;
    }

    try {
        const response = await fetch("/user/chats", {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${jwt}`,
            },
        });

        if (response.ok) {
            const chats = await response.json();
            console.log(chats);
            displayChats(chats);
        } else {
            const error = await response.json();
            showError(error.message || "Failed to fetch chats.");
        }
    } catch (error) {
        showError("Unable to connect to the server.");
    }
}

// Function to display chats on the page
function displayChats(chats) {
    const chatList = document.getElementById("chat-list");
    chatList.innerHTML = ""; // Clear previous content

    if (chats.length === 0) {
        chatList.innerHTML = "<p>No chats available.</p>";
        return;
    }

    chats.forEach(chat => {
        const chatButton = document.createElement("button");
        chatButton.classList.add("chat-button");
        chatButton.textContent = chat.chatName || "Unnamed Chat";
        chatButton.dataset.chatId = chat.id; // Store the chatId for later use

        // Add event listener for future actions (e.g., opening the chat)
        chatButton.addEventListener("click", () => {
            openChat(chat.id);
        });

        chatList.appendChild(chatButton);
    });
}

// Function to handle chat button clicks
function openChat(chatId) {
    console.log("Chat selected:", chatId);
    // Redirect to the chat page
    window.location.href = `/chat/${chatId}`;
}

// Function to show error messages
function showError(message) {
    const errorMessage = document.getElementById("error-message");
    errorMessage.textContent = message;
    errorMessage.style.color = "red";
}

// Load chats on page load
window.addEventListener("DOMContentLoaded", loadChats);
