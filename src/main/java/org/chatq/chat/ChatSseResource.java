package org.chatq.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;

import java.util.Collection;
import java.util.concurrent.SubmissionPublisher;


@Path("/chat/sse")
@ApplicationScoped
public class ChatSseResource {

    private final SubmissionPublisher<Collection<String>> publisher = new SubmissionPublisher<>(Infrastructure.getDefaultExecutor(), 10);

    @Inject
    Sse sse;
    @Inject
    ObjectMapper objectMapper;

    @GET
    @Path("/active-users")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<OutboundSseEvent> streamActiveUsernames() {
        return Multi.createFrom().publisher(publisher)
                .map(activeUsers -> {
                    try {
                        // Converts Collection<String> to String so frontend can decode the JSON
                        String json = objectMapper.writeValueAsString(activeUsers);
                        return sse.newEventBuilder()
                                .name("active-users")
                                .data(json)  // Send valid JSON string
                                .build();
                        // If some problem, username is null and log error
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

    public void streamActiveUsernames(Collection<String> activeUsernames) {
        publisher.submit(activeUsernames);
    }
}
