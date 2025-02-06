package org.chatq.users;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.chatq.auth.TokenResponse;
import org.chatq.auth.TempUser;
import org.chatq.auth.AuthService;

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
        userService.addUser(tempUser.getUsername(), tempUser.getPlainPassword());
        return Response.ok().entity("User registered successfully").build();
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

}
