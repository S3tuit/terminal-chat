package org.chatq;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.chatq.entities.TempUser;
import org.chatq.entities.User;
import org.chatq.service.AuthService;
import org.chatq.service.UserService;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/user")
public class UserResource {

    @Inject
    UserService userService;

    @POST
    @Path("/register")
    public Response addUser(TempUser tempUser) {
        // password is not yet hashed at this time
        if (tempUser == null || tempUser.username == null || tempUser.plainPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username and password are required").build();
        }

        // this method hashes the password before persisting the user
        userService.addUser(tempUser.username, tempUser.plainPassword);
        return Response.ok().entity("User registered successfully").build();
    }

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateUser(TempUser tempUser) {
        if (tempUser.username == null || tempUser.plainPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username and password are required").build();
        }

        User validUser = AuthService.validateLogin(tempUser.username, tempUser.plainPassword);
        if (validUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid username or password").build();
        } else {
            return Response.ok().entity(validUser).build();
        }
    }

}
