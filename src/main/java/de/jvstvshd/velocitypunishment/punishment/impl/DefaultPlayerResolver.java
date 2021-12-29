package de.jvstvshd.velocitypunishment.punishment.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.util.Util;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DefaultPlayerResolver implements PlayerResolver {

    private final ProxyServer proxyServer;

    public DefaultPlayerResolver(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public Optional<String> getPlayerName(@NotNull UUID uuid) {
        Optional<Player> optional = proxyServer.getPlayer(uuid);
        if (optional.isEmpty())
            return Optional.empty();
        return Optional.ofNullable(optional.get().getUsername());
    }

    @Override
    public CompletableFuture<String> queryPlayerName(@NotNull UUID uuid, @NotNull Executor executor) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonElement jsonElement = JsonParser.parseString(response.body());
                cf.complete(jsonElement.getAsJsonObject().get("name").getAsString());
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    @Override
    public CompletableFuture<String> getOrQueryPlayerName(@NotNull UUID uuid, @NotNull Executor executor) {
        if (getPlayerName(uuid).isPresent()) {
            return CompletableFuture.completedFuture(getPlayerName(uuid).get());
        }
        return queryPlayerName(uuid, executor);
    }

    @Override
    public Optional<UUID> getPlayerUuid(@NotNull String name) {
        Optional<Player> optional = proxyServer.getPlayer(name);
        if (optional.isEmpty())
            return Optional.empty();
        return Optional.ofNullable(optional.get().getUniqueId());
    }

    @Override
    public CompletableFuture<UUID> queryPlayerUuid(@NotNull String name, @NotNull Executor executor) {
        CompletableFuture<UUID> cf = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonElement jsonElement = JsonParser.parseString(response.body());
                if (jsonElement == null || jsonElement.isJsonNull()) {
                    cf.complete(null);
                    return;
                }
                cf.complete(Util.parseUuid(jsonElement.getAsJsonObject().get("id").getAsString()));
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    @Override
    public CompletableFuture<UUID> getOrQueryPlayerUuid(@NotNull String name, @NotNull Executor executor) {
        if (getPlayerUuid(name).isPresent()) {
            return CompletableFuture.completedFuture(getPlayerUuid(name).get());
        }
        return queryPlayerUuid(name, executor);
    }
}