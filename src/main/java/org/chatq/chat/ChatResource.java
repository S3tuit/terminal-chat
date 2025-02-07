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
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Collection;
import java.util.List;


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
    @Inject
    ChatService chatService;

    @GET
    @Path("/active-users")
    public Collection<String> getActiveUsernames() {
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

        if (ctx.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (userService.hasAccessToChat(ctx.getUserPrincipal().getName(), chatId)) {
            List<ChatMessage> chatMessages = chatMessageService.getChatMessages(chatId, page);
            return Response.ok(chatMessages).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
    }

    // Create a new Chat entity and assign its ObjectId to chatIds of the User who created it
    @POST
    @Path("/create-chat")
    @RolesAllowed({"User"})
    public Response createChat(@Context SecurityContext ctx, Chat chat) {
        // Check for token validity
        if (ctx.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        JsonWebToken jwt = (JsonWebToken) ctx.getUserPrincipal();
        if(jwt.getClaim("userId") == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Check for request validity
        if (chat == null || chat.direct == null || chat.chatName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Seems like that's not a valid chat").build();
        }

        // Create a new Chat entity and its id to the user who created it
        Chat createdChat = chatService.createChat(chat.direct, chat.chatName, jwt.getClaim("userId").toString());
        if (createdChat != null && userService.addChatToUser(jwt.getClaim("userId").toString(), createdChat.id)) {
            return Response.status(Response.Status.CREATED).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went on our end, sorry").build();
        }
    }
}
