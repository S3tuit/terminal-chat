package org.chatq;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.chatq.entities.User;
import org.chatq.service.AuthService;
import org.chatq.service.UserService;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/user")
public class UserResource {

    @Inject
    UserService userService;

    @POST
    @Path("/register")
    public Response addUser(User user) {
        // password is not yet hashed at this time
        if (user == null || user.username == null || user.hashedPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username and password are required").build();
        }

        // this method hashes the password before persisting the user
        userService.addUser(user.username, user.hashedPassword);
        return Response.ok().entity("User registered successfully").build();
    }

    @GET
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateUser(@QueryParam("username") String username, @QueryParam("password") String password) {
        if (username == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username and password are required").build();
        }

        User validUser = AuthService.validateLogin(username, password);
        if (validUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid username or password").build();
        } else {
            return Response.ok().entity(validUser).build();
        }
    }

}
