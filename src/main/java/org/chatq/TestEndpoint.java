package org.chatq;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.chatq.chat.ChatSocket;
import org.chatq.chat.SessionRepository;
import org.chatq.users.UserRepository;

import java.util.Set;

@ApplicationScoped
@Path("test")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TestEndpoint {

    @Inject
    UserRepository userRepository;
    @Inject
    SessionRepository sessionRepository;
    @Inject
    ChatSocket chatSocket;

    @GET
    @Path("getchatIds")
    public Uni<Set<ObjectId>> getChatIds(@Context SecurityContext ctx) {
        return userRepository.getChatIds(ctx.getUserPrincipal().getName());
    }

    @POST
    @Path("redis")
    public Uni<Boolean> addRedis(@QueryParam("username") String username,
                              @QueryParam("sessionId") String sessionId) {
        return chatSocket.storeSession(sessionId, username);
    }



}
