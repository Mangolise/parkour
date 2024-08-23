package net.mangolise.parkour.command;

import net.mangolise.parkour.MapData;
import net.mangolise.parkour.ParkourUtil;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public class CheckpointCommand extends Command {
    public CheckpointCommand() {
        super("checkpoint");

        // TODO: Add permissions
        setCondition((sender, s) -> sender instanceof Player);

        addSyntax(this::execute, ArgumentType.Integer("checkpoint"));
        addSyntax(this::execute, ArgumentType.Integer("checkpoint"), ArgumentType.Integer("which"));
    }

    private void execute(CommandSender sender, CommandContext context) {
        int index = context.get("checkpoint");
        int which = context.getOrDefault("which", 0);

        if (!(sender instanceof Player player)) {
            sender.sendMessage("must be player!");
            return;
        }

        Pos checkpoint;
        try {
            checkpoint = MapData.checkpoints.get(index).get(which);
        } catch (IndexOutOfBoundsException ignored) {
            sender.sendMessage("wrong checkpoint!");
            return;
        }

        player.teleport(checkpoint);
        ParkourUtil.setCheckpoint(player, checkpoint, index);
    }
}
