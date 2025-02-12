package org.chatq.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import org.chatq.auth.AuthService;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


@Path("/chat/sse")
@ApplicationScoped
public class ChatSseResource {

    @Inject
    ObjectMapper objectMapper;
    @Inject
    AuthService authService;

    private final ConcurrentHashMap<String, SseBroadcaster> broadcastersMap = new ConcurrentHashMap<>();
    @Context
    private Sse sse;


    @GET
    @Path("{chatId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context SseEventSink eventSink, @PathParam("chatId") String chatId, @QueryParam("token") String token) {
        if (authService.getUsernameIfPermission(token, chatId) == null) {
            eventSink.close();
            return;
        }
        System.out.println("Registering sseEventSink at " + chatId);
        broadcastersMap.computeIfAbsent(chatId, broadcaster -> sse.newBroadcaster())
                .register(eventSink);
        // send a message just to the newly created client eventSink.send(sse.newEvent("Hello bro"));
    }

    @POST
    public void broadcast(BroadcastParam broadcastParam) {
        OutboundSseEvent event = sse.newEventBuilder().data(broadcastParam.message).reconnectDelay(10_000).build();
        SseBroadcaster broadcaster = broadcastersMap.get(broadcastParam.chatId);
        if (broadcaster != null) {
            broadcaster.broadcast(event);
        }
    }


    public void streamActiveUsernames(Collection<String> activeUsernames, String chatId) {
        if (activeUsernames == null || chatId == null) {
            return;
        }
        String message;
        try {
            message = objectMapper.writeValueAsString(activeUsernames);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

        broadcast(new BroadcastParam(chatId, message));
    }

    public class BroadcastParam {
        String chatId;
        String message;

        public BroadcastParam(String chatId, String message) {
            this.chatId = chatId;
            this.message = message;
        }
    }
}
