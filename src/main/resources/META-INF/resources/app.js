var connected = false;
var socket;
var eventSource;

// When the document is ready
$(document).ready(function () {
    $("#connect").click(connect);
    $("#send").click(sendMessage);

    // Handling pressing 'Enter' key to trigger connection or message send
    $("#name").keypress(function (event) {
        if (event.keyCode === 13 || event.which === 13) {
            connect();
        }
    });

    $("#msg").keypress(function (event) {
        if (event.keyCode === 13 || event.which === 13) {
            sendMessage();
        }
    });

    // Ensures chat scrolls to the bottom
    $("#chat").change(function () {
        scrollToBottom();
    });

    $("#name").focus();

    // Initialize SSE connection
    initializeSSE();
});

// Connect to WebSocket
var connect = function () {
    if (!connected) {
        var name = $("#name").val();
        console.log("Val: " + name);
        socket = new WebSocket("ws://" + location.host + "/chat/" + name + "/67a0d66085787a5c95785e99");
        socket.onopen = function () {
            connected = true;
            console.log("Connected to the web socket");
            $("#send").attr("disabled", false);
            $("#connect").attr("disabled", true);
            $("#name").attr("disabled", true);
            $("#msg").focus();
        };
        socket.onmessage = function (m) {
            console.log("Got message: " + m.data);
            $("#chat").append(m.data + "\n");
            scrollToBottom();
        };
    }
};

// Send message to WebSocket
var sendMessage = function () {
    if (connected) {
        var value = $("#msg").val();
        console.log("Sending " + value);
        socket.send(value);
        $("#msg").val("");
    }
};

// Scroll the chat text area to the bottom
var scrollToBottom = function () {
    $('#chat').scrollTop($('#chat')[0].scrollHeight);
};

// Initialize SSE connection to fetch active users in real time
var initializeSSE = function () {
    eventSource = new EventSource("http://" + location.host + "/sse/active-users");

    // Listen for active users' updates
    eventSource.addEventListener("active-users", function (event) {
        var activeUsers = JSON.parse(event.data);  // Assuming the server sends an array of active users
        $("#active-users").val(activeUsers.join("\n")); // Display active users in the textarea
    });

    // Listen for incoming chat messages
    eventSource.addEventListener("chat-message", function (event) {
        console.log("New message received: " + event.data);
        $("#chat").append(event.data + "\n");
        scrollToBottom();
    });

    // Handle SSE connection errors
    eventSource.onerror = function () {
        console.error("Error with SSE connection.");
    };

};
