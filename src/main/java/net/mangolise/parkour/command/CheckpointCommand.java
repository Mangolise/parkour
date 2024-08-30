package net.mangolise.parkour.command;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.parkour.MapData;
import net.mangolise.parkour.ParkourUtil;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public class CheckpointCommand extends MangoliseCommand {
    public CheckpointCommand() {
        super("checkpoint");

        addPlayerSyntax(this::execute, ArgumentType.Integer("checkpoint"));
        addPlayerSyntax(this::execute, ArgumentType.Integer("checkpoint"), ArgumentType.Integer("which"));
    }

    private void execute(Player sender, CommandContext context) {
        int index = context.get("checkpoint");
        int which = context.getOrDefault("which", 0);

        Pos checkpoint;
        try {
            checkpoint = MapData.checkpoints.get(index).get(which);
        } catch (IndexOutOfBoundsException ignored) {
            sender.sendMessage("wrong checkpoint!");
            return;
        }

        sender.teleport(checkpoint);
        ParkourUtil.setCheckpoint(sender, ParkourUtil.getData(sender), checkpoint, index);
    }

    @Override
    protected String getPermission() {
        return "mangolise.command.parkour.checkpoint";
    }
}
