package org.chatq.invite;

import io.smallrye.mutiny.Uni;
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

import org.chatq.invite.InviteRepository.TimePeriod;
import org.chatq.users.UserRepository;

@ApplicationScoped
@Path("/invite")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InviteService {

    @Inject
    InviteRepository inviteRepository;
    @Inject
    UserRepository userRepository;

    @POST
    @Path("/create-invite")
    @RolesAllowed({"User"})
    public Uni<Response> createInvite(Chat chat, @Context SecurityContext ctx,
                                      @QueryParam("timePeriod") TimePeriod timePeriod) {
        if (chat == null || chat.id == null || timePeriod == null) {
            return Uni.createFrom().item(Response
                    .status(Response.Status.BAD_REQUEST).entity("Something's missing, we can't create an invite").build());
        }

        if (ctx.getUserPrincipal() == null) {
            return Uni.createFrom().item(Response
                    .status(Response.Status.UNAUTHORIZED).build());
        }

        // If the user has access to that chat create and return an Invite for that Chat
        return userRepository.hasChat(ctx.getUserPrincipal().getName(), chat.id)
                .flatMap(hasChat -> {
                    if (hasChat) {
                        return inviteRepository.createInvite(chat.id, timePeriod)
                                .onItem().ifNotNull().transform(invite -> {
                                    return Response.status(Response.Status.CREATED).entity(invite).build();
                                })
                                .onItem().ifNull().continueWith(
                                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                .entity("We weren't able to create an invite, sry...")
                                                .build()
                                );
                    }
                    return Uni.createFrom().item(Response
                            .status(Response.Status.UNAUTHORIZED).build());
                })
                .onFailure().recoverWithItem(th ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Something went on our end: " + th.getMessage())
                                .build()
                );
    }

    @POST
    @Path("/invite-user")
    @RolesAllowed({"User"})
    public Uni<Response> inviteUser(Invite invite, @Context SecurityContext ctx) {
        // Check for token validity
        String userId = AuthService.getClaimFromCtx(ctx, "userId");
        if (userId == null) {
            return Uni.createFrom().item(Response
                .status(Response.Status.UNAUTHORIZED).build());
        }

        // Check for request validity
        if (invite == null || invite.code == null) {
            return Uni.createFrom().item(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Trying to invite with a null inviteCode?").build());
        }

        return inviteRepository.inviteUserToChat(invite.code, userId)
                .map(userInvited -> {
                    if (userInvited) {
                        return Response.status(Response.Status.CREATED).build();
                    }
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("User NOT invited in the chat.")
                            .build();
                });
    }

}
