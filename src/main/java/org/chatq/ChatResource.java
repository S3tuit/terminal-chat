package org.chatq;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.chatq.socket.ChatSocket;

import java.util.Set;


@Path("/api")
public class ChatResource {

    @Inject
    ChatSocket chatSocket;

    @GET
    @Path("/active-users")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getActiveUsernames() {
        return chatSocket.getActiveUsernames();
    }
}
