package org.chatq.invite;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.chatq.auth.AuthService;
import org.chatq.chat.Chat;
import org.chatq.users.User;

import org.chatq.invite.InviteService.TimePeriod;

@ApplicationScoped
@Path("/invite")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InviteResource {

    @Inject
    InviteService inviteService;

    @POST
    @Path("/create-invite")
    @RolesAllowed({"User"})
    public Response createInvite(Chat chat, @Context SecurityContext ctx,
                                 @QueryParam("timePeriod") TimePeriod timePeriod) {
        if (chat == null || chat.id == null || timePeriod == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Something's missing, we can't create an invite").build();
        }

        if (ctx.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (User.hasChat(ctx.getUserPrincipal().getName(), chat.id)) {
            Invite invite = inviteService.createInvite(chat.id, timePeriod);
            return Response.status(Response.Status.CREATED).entity(invite).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @POST
    @Path("/invite-user")
    @RolesAllowed({"User"})
    public Response inviteUser(Invite invite, @Context SecurityContext ctx) {
        // Check for token validity
        String userId = AuthService.getClaimFromCtx(ctx, "userId");
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Check for request validity
        if (invite == null || invite.code == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Trying to invite with a null inviteCode?").build();
        }

        if (inviteService.inviteUserToChat(invite.code, userId)) {
            return Response.status(Response.Status.CREATED).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Oops... user did not get invited to the chat").build();
        }
    }
}
