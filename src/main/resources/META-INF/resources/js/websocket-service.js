let socket = null;
let messageHandlers = {}; // To store message handlers for different message types

export const connectWebSocket = (token) => {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        socket = new WebSocket(`ws://${location.host}/chat/ws/${token}`);

        socket.onopen = () => {
            console.log("WebSocket connection opened");
        };

        socket.onmessage = (event) => {
            handleWebSocketMessage(event.data);
        };

        socket.onclose = () => {
            console.log("WebSocket connection closed");
            socket = null; // Reset socket
        };

        socket.onerror = (error) => {
            console.error("WebSocket error:", error);
        };
    }
    return socket;
};

export const getWebSocket = () => {
    return socket;
};

export const sendMessage = (message) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(message);
    } else {
        console.error("WebSocket not open, cannot send message:", message);
    }
};

// Function to register message handlers
export const registerMessageHandler = (messageType, handlerFunction) => {
    messageHandlers[messageType] = handlerFunction;
};

const handleWebSocketMessage = (messageData) => {
    try {
        const message = JSON.parse(messageData);
        const messageType = message.type; // Assuming your JSON messages have a 'type' field

        if (messageHandlers[messageType]) {
            messageHandlers[messageType](message); // Call the registered handler for this type
        } else {
            console.warn("No handler registered for message type:", messageType, message);
        }
    } catch (error) {
        console.error("Error parsing WebSocket message:", error, messageData);
    }
};