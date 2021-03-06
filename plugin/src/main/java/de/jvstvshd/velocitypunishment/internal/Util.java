/*
 * This file is part of Velocity Punishment, which is licensed under the MIT license.
 *
 * Copyright (c) 2022 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.jvstvshd.velocitypunishment.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.TemporalPunishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Util {

    public static List<String> getPlayerNames(Collection<Player> players) {
        return players.stream().collect(new Collector<Player, List<Player>, List<String>>() {
            @Override
            public Supplier<List<Player>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<Player>, Player> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<List<Player>> combiner() {
                return (players, players2) -> {
                    players.addAll(players2);
                    return players;
                };
            }

            @Override
            public Function<List<Player>, List<String>> finisher() {
                return players -> {
                    List<String> strings = new ArrayList<>();
                    for (Player player : players) {
                        strings.add(player.getUsername());
                    }
                    return strings;
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.UNORDERED);
            }
        });
    }

    public static UUID parseUuid(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return UUID.fromString(uuidString.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"));
        }
    }

    public static TextComponent copyComponent(String text, MessageProvider provider, CommandSource source) {
        return Component.text(text).clickEvent(ClickEvent.suggestCommand(text))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(provider.provide("commands.general.copy", source).color(NamedTextColor.GREEN)));
    }

    public static <T> CompletableFuture<T> executeAsync(Callable<T> task, Executor service) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        service.execute(() -> {
            try {
                cf.complete(task.call());
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    public static <T extends TemporalPunishment> T getLongestPunishment(List<T> list) {
        if (list.isEmpty())
            return null;
        List<T> sorted = sortPunishments(list);
        return sorted.get(sorted.size() - 1);
    }

    public static <T extends TemporalPunishment> List<T> sortPunishments(List<T> list) {
        return list.stream().sorted(Comparator.comparing(TemporalPunishment::getDuration)).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Punishment> List<T> convert(List<? super T> list) {
        List<T> out = new ArrayList<>();
        for (Object o : list) {
            out.add((T) o);
        }
        return out;
    }

    public static String trimUuid(UUID origin) {
        return origin.toString().toLowerCase().replace("-", "");
    }

    public static boolean sendErrorMessageIfErrorOccurred(SimpleCommand.Invocation invocation, CommandSource source, UUID uuid, Throwable throwable, VelocityPunishmentPlugin plugin) {
        if (throwable != null) {
            source.sendMessage(plugin.getMessageProvider().internalError(source, true));
            throwable.printStackTrace();
            return true;
        }
        if (uuid == null) {
            source.sendMessage(Component.translatable().args(Component.text(invocation.arguments()[0]).color(NamedTextColor.YELLOW)).key("commands.general.not-found").color(NamedTextColor.RED));
            return true;
        }
        return false;
    }

    public static List<String> getPlayerNames(SimpleCommand.Invocation invocation, ProxyServer proxyServer) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return Util.getPlayerNames(proxyServer.getAllPlayers());
        }
        if (args.length == 1) {
            return Util.getPlayerNames(proxyServer.getAllPlayers())
                    .stream().filter(s -> s.toLowerCase().startsWith(args[0])).collect(Collectors.toList());
        }
        return ImmutableList.of();
    }
}
