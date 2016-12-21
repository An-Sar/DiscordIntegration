/**
 * Copyright (C) 2016 Chikachi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package chikachi.discord.command.discord;

import chikachi.discord.DiscordClient;
import chikachi.discord.DiscordTeleporter;
import chikachi.discord.experimental.DiscordFakePlayer;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.entities.User;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraftforge.common.DimensionManager;

import java.util.List;

public class UnstuckCommandConfig extends CommandConfig {
    public UnstuckCommandConfig() {
        super("unstuck", false, "AdminRoleHere");
    }

    @Override
    public void execute(MinecraftServer minecraftServer, User user, List<String> args) {
        if (args.size() == 0) {
            DiscordClient.getInstance().sendMessage("Missing player");
            return;
        }
        String playerName = args.remove(0);
        if (playerName.length() == 0) {
            DiscordClient.getInstance().sendMessage("Missing player");
            return;
        }

        WorldServer overworld = DimensionManager.getWorld(0);
        BlockPos spawnpoint = overworld.getSpawnPoint();
        IBlockState blockState = overworld.getBlockState(spawnpoint);

        while (blockState.getBlock().isOpaqueCube(blockState)) {
            spawnpoint = spawnpoint.up(2);
            blockState = overworld.getBlockState(spawnpoint);
        }

        double x = spawnpoint.getX() + 0.5;
        double y = spawnpoint.getY();
        double z = spawnpoint.getZ() + 0.5;

        PlayerList playerList = minecraftServer.getPlayerList();
        EntityPlayerMP playerEntity = playerList.getPlayerByUsername(playerName);

        if (playerEntity != null) {
            int fromDimId = playerEntity.dimension;

            if (fromDimId != 0) {
                playerList.transferPlayerToDimension(playerEntity, 0, new DiscordTeleporter(overworld));

                if (fromDimId == 1 && playerEntity.isEntityAlive()) {
                    overworld.spawnEntity(playerEntity);
                    overworld.updateEntityWithOptionalForce(playerEntity, false);
                }
            }

            playerEntity.setPositionAndUpdate(x, y, z);
            //playerEntity.connection.kickPlayerFromServer("You are getting sent to spawn, please connect again!");
        } else {
            GameProfile playerProfile = minecraftServer.getPlayerProfileCache().getGameProfileForUsername(playerName);

            if (playerProfile == null || !playerProfile.isComplete()) {
                DiscordClient.getInstance().sendMessage("Player not found");
                return;
            }

            //DiscordFakePlayer fakePlayer = new DiscordFakePlayer(minecraftServer.worldServers[0], playerProfile);
            DiscordFakePlayer fakePlayer = new DiscordFakePlayer(minecraftServer.worldServerForDimension(0), playerProfile);
            //IPlayerFileData saveHandler = minecraftServer.worldServers[0].getSaveHandler().getPlayerNBTManager();
            IPlayerFileData saveHandler = minecraftServer.worldServerForDimension(0).getSaveHandler().getPlayerNBTManager();
            NBTTagCompound playerData = saveHandler.readPlayerData(fakePlayer);

            if (playerData == null) {
                DiscordClient.getInstance().sendMessage("Player not found on server");
                return;
            }

            fakePlayer.posX = x;
            fakePlayer.posY = y;
            fakePlayer.posZ = z;

            fakePlayer.dimension = 0;

            saveHandler.writePlayerData(fakePlayer);
        }

        DiscordClient.getInstance().sendMessage("Player sent to spawn");
    }
}
