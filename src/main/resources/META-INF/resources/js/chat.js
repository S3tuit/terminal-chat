import {getWebSocket, sendMessage, registerMessageHandler, connectWebSocket} from './websocket-service.js';

// Get currChatId from the URL
const currChatId = window.location.pathname.split("/").pop();
const messageBox = document.getElementById("message-box");
const messageInput = document.getElementById("message-input");
const sendBtn = document.getElementById("send-btn");
const activeUsersList = document.getElementById("active-users-list");
let socket;

// Get the JWT token
const token = localStorage.getItem("jwt");

if (!token) {
    alert("You are not logged in. Please log in to access the chat.");
    window.location.href = ""; // Redirect to login page
}

// Load chat messages on page load
window.addEventListener("DOMContentLoaded", async () => {
    await loadMessages();
    setupMessageHandlers();

    socket = getWebSocket();
    if (!socket) {
        connectWebSocket(token);
        socket = getWebSocket();
    }
});

// Setup message handlers using the websocket-service
function setupMessageHandlers() {
    registerMessageHandler("ChatMessage", handleChatMessage);
}

function handleChatMessage(incomingMessage) {
    const { message: message, fromUsername, timestamp, chatId } = incomingMessage; // Extract from message

    if (chatId !== currChatId) { return } // Filter for current chatId

    const messageDiv = document.createElement("div");
    messageDiv.classList.add("message");
    messageDiv.classList.add(fromUsername === "You" ? "sent" : "received");

    messageDiv.innerHTML = `
        <p>${message}</p> 
        <p class="timestamp">${fromUsername} - ${new Date(timestamp).toLocaleString()}</p>
    `;
    messageBox.appendChild(messageDiv);
    messageBox.scrollTop = messageBox.scrollHeight;
}

// Function to load messages
async function loadMessages(page = 0) {
    try {
        const response = await fetch(`/chat/messages?chatId=${currChatId}&page=${page}`, {
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
    messages.slice().reverse().forEach(({ message, fromUsername, timestamp }) => {
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


// Function to send a message
sendBtn.addEventListener("click", () => {
    const message = messageInput.value.trim();
    if (message) {

        const messagePayload = {
            chatId: currChatId,
            message: message
        }
        socket.send(JSON.stringify(messagePayload)); // Send message to WebSocket
        messageInput.value = ""; // Clear input
    }
});


// Invite button
// Get references to the dialog and its buttons
const createInviteBtn = document.getElementById("create-invite-btn");
const createInviteDialog = document.getElementById("invite-dialog");
const createInviteConfirmBtn = document.getElementById("create-invite-confirm-btn");
const closeDialogBtn = document.getElementById("invite-dialog-close-btn");
const timePeriodSelect = document.getElementById("time-period-select");

// Function to show the dialog
function showDialog() {
    createInviteDialog.style.display = "flex";
}

// Function to hide the dialog
function hideDialog() {
    createInviteDialog.style.display = "none";
}

// Attach event listeners
createInviteBtn.addEventListener("click", showDialog);
closeDialogBtn.addEventListener("click", hideDialog);

// Handle the Create button in the dialog
createInviteConfirmBtn.addEventListener("click", async () => {
    const timePeriod = timePeriodSelect.value;

    try {
        const response = await fetch(`/invite/create-invite?timePeriod=${timePeriod}`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({id: currChatId}), // pass the currChatId
        });

        if (response.ok) {
            const invite = await response.json();
            alert(`Invite created successfully! Code: ${invite.code}`);
            hideDialog(); // Close the dialog after creating the invite
        } else {
            alert("Failed to create invite. Please try again.");
        }
    } catch (err) {
        console.error("Error creating invite:", err);
        alert("An error occurred. Please try again.");
    }
});


