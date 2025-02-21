package org.chatq.chat;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.chatq.auth.AuthService;
import org.chatq.users.UserRepository;

import java.io.File;


@Path("/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatService {

    @Inject
    ChatSocket chatSocket;
    @Inject
    ChatMessageRepository chatMessageRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    ChatRepository chatRepository;


    @GET
    @RolesAllowed({"User"})
    @Path("messages")
    public Uni<Response> getMessages(@QueryParam("chatId") ObjectId chatId, @QueryParam("page") int page,
                                @Context SecurityContext ctx) {
        if (chatId == null || page < 0) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).entity("Parameters not valid.").build());
        }

        if (ctx.getUserPrincipal() == null) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        return userRepository.hasChat(ctx.getUserPrincipal().getName(), chatId)
                .onItem().ifNotNull().transformToUni(hasAccess -> {
                    if (hasAccess) {
                        return chatMessageRepository.getChatMessagesPage(chatId, page)
                                .onItem().transform(messages ->
                                        Response.ok(messages).build());
                    } else {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.UNAUTHORIZED)
                                        .entity("Access denied.")
                                        .build()
                        );
                    }
                })
                .onFailure().recoverWithItem(th ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Something went on our end: " + th.getMessage())
                                .build()
                );
    }

    // Create a new Chat entity and assign its ObjectId to chatIds of the User who created it
    @POST
    @Path("/create-chat")
    @RolesAllowed({"User"})
    public Uni<Response> createChat(@Context SecurityContext ctx, Chat chat) {
        // Check for token validity
        String userId = AuthService.getClaimFromCtx(ctx, "userId");
        if (userId == null) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // Check for request validity
        if (chat == null || chat.direct == null || chat.chatName == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Seems like that's not a valid chat").build());
        }

        // Create a new Chat entity and assign its id to the user who created it
        return chatRepository.createChat(chat.direct, chat.chatName, userId)
                .onItem().ifNotNull().transform(newChat ->
                        Response.status(Response.Status.CREATED)
                        .entity(newChat).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity("Chat NOT created :(")
                                .build())
                .onFailure().recoverWithItem(th ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Something went on our end: " + th.getMessage()).build()
                );
    }

    // Dynamically generate HTML pages based on chatId
    @GET
    @Path("/{chatId}")
    @Produces(MediaType.TEXT_HTML)
    public Uni<File> serveChatPage(@PathParam("chatId") String chatId) {
        return chatRepository.getChatIdIfExists(chatId)
                .onItem().ifNotNull().transform(ignored -> {
                    File file = new File("src/main/resources/META-INF/resources/chat/chat.html");
                    if (!file.exists()) {
                        throw new WebApplicationException(Response.Status.NOT_FOUND);
                    }
                    return file;
                })
                .onItem().ifNull().failWith(
                        new WebApplicationException(Response.Status.NOT_FOUND)
                );
    }
}
