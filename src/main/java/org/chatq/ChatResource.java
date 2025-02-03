package org.chatq;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.chatq.entities.ChatMessage;
import org.chatq.service.ChatMessageService;
import org.chatq.socket.ChatSocket;

import java.util.List;
import java.util.Set;


@Path("/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ChatResource {

    @Inject
    ChatSocket chatSocket;
    @Inject
    ChatMessageService chatMessageService;

    @GET
    @Path("/active-users")
    public Set<String> getActiveUsernames() {
        return chatSocket.getActiveUsernames();
    }

    @GET
    @Path("{chatId}/messages/{page}")
    public List<ChatMessage> getMessages(@PathParam("chatId") String chatId, @PathParam("page") int page) {
        return chatMessageService.getChatMessages(chatId, page);
    }
}
