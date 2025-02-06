package org.chatq.chat;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.chatq.users.UserService;

import java.util.List;
import java.util.Set;


@Path("/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatResource {

    @Inject
    ChatSocket chatSocket;
    @Inject
    ChatMessageService chatMessageService;
    @Inject
    UserService userService;

    @GET
    @Path("/active-users")
    public Set<String> getActiveUsernames() {
        return chatSocket.getActiveUsernames();
    }

    @GET
    @RolesAllowed({"User"})
    @Path("messages")
    public Response getMessages(@QueryParam("chatId") ObjectId chatId, @QueryParam("page") int page,
                                @Context SecurityContext ctx) {
        if (chatId == null || page < 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Parameters not valid.").build();
        }

        if (userService.hasAccessToChat(ctx.getUserPrincipal().getName(), chatId)) {
            List<ChatMessage> chatMessages = chatMessageService.getChatMessages(chatId, page);
            return Response.ok(chatMessages).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
    }
}
