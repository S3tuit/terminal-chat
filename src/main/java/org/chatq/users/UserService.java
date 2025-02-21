package org.chatq.users;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.chatq.auth.TokenResponse;
import org.chatq.auth.TempUser;
import org.chatq.auth.AuthService;
import org.chatq.chat.ChatMessageRepository;

import java.util.List;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/user")
public class UserService {

    @Inject
    UserRepository userRepository;
    @Inject
    AuthService authService;
    @Inject
    ChatMessageRepository chatMessageRepository;

    @POST
    @Path("/register")
    public Uni<Response> addUser(TempUser tempUser) {
        // password is not yet hashed here
        if (tempUser == null || tempUser.getUsername() == null || tempUser.getPlainPassword() == null) {
            return Uni.createFrom().item(Response
                    .status(Response.Status.BAD_REQUEST).entity("Username and password are required").build());
        }

        // this method hashes the password before persisting the user
        return userRepository.addUser(tempUser.getUsername(), tempUser.getPlainPassword())
                .onItem().ifNotNull().transform(user ->
                        Response.ok(user)
                                .build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(Response.Status.UNAUTHORIZED).build()
                        )
                .onFailure().recoverWithItem(th ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Something went on our end: " + th.getMessage())
                                .build()
                );
    }

    @POST
    @Path("/login")
    public Uni<Response> validateUser(TempUser tempUser) {
        if (tempUser.getUsername() == null || tempUser.getPlainPassword() == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new TokenResponse("Username and password are required")).build());
        }

        return authService.validateLogin(tempUser.getUsername(), tempUser.getPlainPassword())
                .onItem().ifNotNull().transform(token ->
                        Response.ok(token)
                                .build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(Response.Status.UNAUTHORIZED)
                                .entity(new TokenResponse("Invalid username or password"))
                                .build()
                )
                .onFailure().recoverWithItem(th ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Something went on our end: " + th.getMessage())
                        .build()
                );
    }

    @GET
    @RolesAllowed({"User"})
    @Path("/chats")
    public Uni<Response> getUserChats(@Context SecurityContext ctx) {
        if (ctx.getUserPrincipal() == null) {
            return Uni.createFrom().item(Response
                    .status(Response.Status.UNAUTHORIZED).build());
        }
        String username = ctx.getUserPrincipal().getName();
        if (username == null) {
            return  Uni.createFrom().item(Response
                    .status(Response.Status.BAD_REQUEST).entity("Ouch, we couldn't find your profile").build());
        }

        return chatMessageRepository.getChatsAndLatestMsg(username)
                .onItem().ifNotNull().transform(chats ->
                        Response.ok().entity(chats).build()
                )
                .onFailure().recoverWithItem(th ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Something went on our end: " + th.getMessage())
                                .build()
                );
    }

}
