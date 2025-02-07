package org.chatq.users;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.chatq.auth.TokenResponse;
import org.chatq.auth.TempUser;
import org.chatq.auth.AuthService;

import java.util.Set;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/user")
public class UserResource {

    @Inject
    UserService userService;
    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    public Response addUser(TempUser tempUser) {
        // password is not yet hashed here
        if (tempUser == null || tempUser.getUsername() == null || tempUser.getPlainPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username and password are required").build();
        }

        // this method hashes the password before persisting the user
        if (userService.addUser(tempUser.getUsername(), tempUser.getPlainPassword())){
            return Response.status(Response.Status.CREATED).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong during registration").build();
        }

    }

    @POST
    @Path("/login")
    public Response validateUser(TempUser tempUser) {
        if (tempUser.getUsername() == null || tempUser.getPlainPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new TokenResponse("Username and password are required")).build();
        }

        TokenResponse token = authService.validateLogin(tempUser.getUsername(), tempUser.getPlainPassword());
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new TokenResponse("Invalid username or password")).build();
        } else {
            return Response.ok().entity(token).build();
        }
    }

    @GET
    @RolesAllowed({"User"})
    @Path("/chats")
    public Response getUserChats(@Context SecurityContext ctx) {
        if (ctx.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String username = ctx.getUserPrincipal().getName();
        if (username == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Ouch, we couldn't find your profile").build();
        }

        Set<ObjectId> chatIds = userService.getUserChatIds(username);
        return Response.ok().entity(chatIds).build();
    }

}
