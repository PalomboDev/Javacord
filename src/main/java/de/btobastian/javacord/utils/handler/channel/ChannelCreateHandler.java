package de.btobastian.javacord.utils.handler.channel;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.entities.channels.ServerTextChannel;
import de.btobastian.javacord.entities.impl.ImplServer;
import de.btobastian.javacord.entities.impl.ImplUser;
import de.btobastian.javacord.events.server.channel.ServerChannelCreateEvent;
import de.btobastian.javacord.listeners.server.channel.ServerChannelCreateListener;
import de.btobastian.javacord.utils.PacketHandler;
import de.btobastian.javacord.utils.logging.LoggerUtil;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the channel create packet.
 */
public class ChannelCreateHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ChannelCreateHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public ChannelCreateHandler(DiscordApi api) {
        super(api, true, "CHANNEL_CREATE");
    }

    @Override
    public void handle(JSONObject packet) {
        int type = packet.getInt("type");
        switch (type) {
            case 0:
                handleServerTextChannel(packet);
                break;
            case 1:
                handlePrivateChannel(packet);
                break;
        }
    }

    /**
     * Handles server channel creation.
     *
     * @param channel The channel data.
     */
    private void handleServerTextChannel(JSONObject channel) {
        long serverId = Long.parseLong(channel.getString("guild_id"));
        api.getServerById(serverId).ifPresent(server -> {
            ServerTextChannel textChannel = ((ImplServer) server).getOrCreateServerTextChannel(channel);
            ServerChannelCreateEvent event = new ServerChannelCreateEvent(api, server, textChannel);
            listenerExecutorService.submit(() -> {
                List<ServerChannelCreateListener> listeners = new ArrayList<>();
                listeners.addAll(server.getServerChannelCreateListeners());
                listeners.addAll(api.getServerChannelCreateListeners());
                listeners.forEach(listener -> listener.onServerChannelCreate(event));
            });
        });
    }

    /**
     * Handles a private channel creation.
     *
     * @param channel The channel data.
     */
    private void handlePrivateChannel(JSONObject channel) {
        // A CHANNEL_CREATE packet is sent every time a bot account receives a message, see
        // https://github.com/hammerandchisel/discord-api-docs/issues/184
        ImplUser recipient = (ImplUser) api.getOrCreateUser(channel.getJSONArray("recipients").getJSONObject(0));
        if (!recipient.getPrivateChannel().isPresent()) {
            recipient.getOrCreateChannel(channel);
        }
    }

}