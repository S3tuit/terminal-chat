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
            displayChats(chats);
        } else {
            const error = await response.json();
            showError(error.message || "Failed to fetch chats.");
        }
    } catch (error) {
        showError("Unable to connect to the server.");
    }
}

function truncateMessage(message) {
    // Limit the snippet to 50 characters
    return message.length > 50 ? message.substring(0, 50) + "..." : message;
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
        chatButton.dataset.chatId = chat.id; // Store the chatId for later use

        const chatName = chat.chatName || "Ghost chat Uuuu...";
        const mostRecentMessage = chat.mostRecentMessage ? `${chat.mostRecentMessage.fromUsername}: ${truncateMessage(chat.mostRecentMessage.message)}` : "No messages yet...";

        chatButton.innerHTML = `
            <div><strong>${chatName}</strong></div>
            <div class="message-snippet">${mostRecentMessage}</div>
        `;

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

// Function to show the create chat dialog
function showCreateChatDialog() {
    document.getElementById("create-chat-dialog").classList.remove("hidden");
}

// Function to hide the create chat dialog
function hideCreateChatDialog() {
    document.getElementById("create-chat-dialog").classList.add("hidden");
}

// Function to hide the enter an invite dialog
function hideEnterInviteDialog() {
    document.getElementById("enter-invite-dialog").classList.add("hidden");
}

// Function to show the invite dialog
function showEnterInviteDialog() {
    document.getElementById("enter-invite-dialog").classList.remove("hidden");
}

// Function to create a new chat
async function createChat() {
    const chatName = document.getElementById("chat-name").value.trim();
    const direct = false;
    if (chatName.length < 2 || chatName.length > 55) {
        showError("Chat name must be between 2 and 55 characters.");
        return;
    }

    const jwt = localStorage.getItem("jwt");
    if (!jwt) {
        window.location.href = "/"; // Redirect to login if no token
        return;
    }

    try {
        const response = await fetch("/chat/create-chat", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${jwt}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({chatName, direct}),
        });

        if (response.status === 201) { // Created
            alert("Chat created successfully!");
            hideCreateChatDialog();
            loadChats(); // Reload the chats
        } else {
            const error = await response.json();
            showError(error.message || "Failed to create chat.");
        }
    } catch (error) {
        showError("Unable to connect to the server.");
    }
}

// Function to get invited to a new chat
async function getInvited() {
    const inviteCode = document.getElementById("invite-code").value.trim();
    if (inviteCode.length < 9) {
        showError("Invite Code too short.");
        return;
    }

    const jwt = localStorage.getItem("jwt");
    if (!jwt) {
        window.location.href = "/"; // Redirect to login if no token
        return;
    }

    try {
        const response = await fetch("/invite/invite-user", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${jwt}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({code: inviteCode}),
        });

        if (response.status === 201) { // Created
            alert("You entered a new chat!");
            hideEnterInviteDialog();
            loadChats(); // Reload the chats
        } else {
            const error = await response.json();
            showError(error.message || "Invite Code not valid.");
        }
    } catch (error) {
        showError("Unable to connect to the server.");
    }
}

// Event listeners

// Create new chat
document.getElementById("create-chat-button").addEventListener("click", showCreateChatDialog);
document.getElementById("cancel-chat-button").addEventListener("click", hideCreateChatDialog);
document.getElementById("submit-chat-button").addEventListener("click", createChat);

// Enter an invite
document.getElementById("enter-invite-button").addEventListener("click", showEnterInviteDialog);
document.getElementById("cancel-invite-code-button").addEventListener("click", hideEnterInviteDialog);
document.getElementById("submit-invite-code-button").addEventListener("click", getInvited);

// Load chats on page load
window.addEventListener("DOMContentLoaded", loadChats);
