package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.command.argument.ArgumentHelper.*;
import static io.th0rgal.oraxen.command.argument.CompletionHelper.*;

import java.util.Optional;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;

public class Pack extends OraxenCommand {

    public static final OraxenCommand COMMAND = new Pack();

    public static CommandInfo info() {
        return new CommandInfo("pack", COMMAND, "menu");
    }

    private Pack() {

    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {
        CommandSender sender = info.getSender();

        if (Conditions.reqPerm(OraxenPermission.COMMAND_PACK).isFalse(sender))
            return;

        Optional<Boolean> option0 = restrict(
            get(arguments, 1, ArgumentType.STRING).map(argument -> argument.asString()), "get", "send")
                .map(value -> value.equals("get"));
        if (!option0.isPresent()) {
            info.getInfo().sendSimple(sender, info.getLabel());
            return;
        }

        Consumer<Player> send = option0.get() ? PackDispatcher::sendWelcomeMessage
            : PackDispatcher::sendPack;
        Optional<Player[]> players = get(arguments, 2, argument -> players(sender, argument));

        if (players.isPresent()) {
            forEach(players, send);
            return;
        }
        if (Conditions.player(Message.NOT_PLAYER).isTrue(sender))
            send.accept((Player) sender);
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        DefaultCompletion completion = new DefaultCompletion();

        if (Conditions.hasPerm(OraxenPermission.COMMAND_RELOAD).isFalse(info.getSender()))
            return completion;

        int count = arguments.count();

        if (count == 1) {
            completion(completion, "get", "send");
        } else if (count == 2) {
            completion(completion,
                Conditions.player().isTrue(info.getSender()) ? (new String[] { "@a", "@r", "@s", "@p" })
                    : (new String[] { "@a", "@r", "@p" }));
            Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            for (int index = 0; index < players.length; index++)
                completion.add(new StringArgument(players[index].getName()));
        }

        return completion;
    }

}
