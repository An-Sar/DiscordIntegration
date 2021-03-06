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

package chikachi.discord.config.message;

import chikachi.discord.Utils;
import com.google.common.base.Joiner;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.common.ForgeHooks;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DiscordChatMessageConfig extends BaseMessageConfig {
    private final MinecraftServer minecraftServer;
    private int maxChatLength = -1;
    private TextFormatting usernameColor = TextFormatting.RED;
    private boolean customFormatting = false;

    private static Map<String, String> emoteMap;

    static {
        emoteMap = new HashMap<>();
        emoteMap.put(":slight_smile:", ":)");
        emoteMap.put(":smile:", ":D");
        emoteMap.put(":wink:", ";)");
        emoteMap.put(":wink:", ";D");
        emoteMap.put(":laughing:", "xD");
        emoteMap.put(":laughing:", "XD");
        emoteMap.put(":stuck_out_tongue:", ":p");
        emoteMap.put(":stuck_out_tongue:", ":P");
        emoteMap.put(":stuck_out_tongue_winking_eye:", ";p");
        emoteMap.put(":stuck_out_tongue_winking_eye:", ";P");
        emoteMap.put(":stuck_out_tongue_closed_eyes:", "xp");
        emoteMap.put(":stuck_out_tongue_closed_eyes:", "xP");
        emoteMap.put(":stuck_out_tongue_closed_eyes:", "Xp");
        emoteMap.put(":stuck_out_tongue_closed_eyes:", "XP");
        emoteMap.put(":open_mouth:", ":O");
        emoteMap.put(":open_mouth:", ":o");
        emoteMap.put(":dizzy_face:", "xO");
        emoteMap.put(":dizzy_face:", "XO");
        emoteMap.put(":neutral_face:", ":|");
        emoteMap.put(":sunglasses:", "B)");
        emoteMap.put(":kissing:", ":*");
        emoteMap.put(":sob:", ";.;");
        emoteMap.put(":cry:", ";_;");
        emoteMap.put(":heart:", "<3");
        emoteMap.put(":broken_heart:", "</3");
        emoteMap.put(":thumbsup:", "(y)");
        emoteMap.put(":thumbsup:", "(Y)");
        emoteMap.put(":thumbsup:", "(yes)");
        emoteMap.put(":thumbsdown:", "(n)");
        emoteMap.put(":thumbsdown:", "(N)");
        emoteMap.put(":thumbsdown:", "(no)");
        emoteMap.put(":ok_hand:", "(ok)");
    }

    public DiscordChatMessageConfig(MinecraftServer minecraftServer, boolean enabled, String message) {
        super("chat", enabled, message);
        this.minecraftServer = minecraftServer;
    }

    private ITextComponent attachmentToTextComponent(Message.Attachment attachment) {
        HoverEvent attachmentHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Open link to attachment"));
        ClickEvent attachmentClickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl());

        ITextComponent chatComponent = new TextComponentString("[" + attachment.getFileName() + "]");
        chatComponent.getStyle()
                .setColor(TextFormatting.AQUA)
                .setClickEvent(attachmentClickEvent)
                .setHoverEvent(attachmentHoverEvent);

        return chatComponent;
    }

    public void handleEvent(MessageReceivedEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        String content = event.getMessage().getContent();
        if (this.maxChatLength > -1 && content.length() > this.maxChatLength) {
            content = content.substring(0, this.maxChatLength);
        }

        TextChannel channel = event.getTextChannel();
        List<EntityPlayerMP> players = minecraftServer.getPlayerList().getPlayerList();
        if (players.size() == 0) {
            return;
        }

        if (content.contains("@everyone") && channel.checkPermission(event.getAuthor(), Permission.MESSAGE_MENTION_EVERYONE)) {
            content = Patterns.everyonePattern.matcher(content).replaceAll("$1" + TextFormatting.BLUE + "@everyone" + TextFormatting.RESET);
        }

        content = Patterns.boldPattern.matcher(content).replaceAll(TextFormatting.BOLD + "$1" + TextFormatting.RESET);
        content = Patterns.italicPattern.matcher(content).replaceAll(TextFormatting.ITALIC + "$1" + TextFormatting.RESET);
        content = Patterns.italicMePattern.matcher(content).replaceAll(TextFormatting.ITALIC + "$1" + TextFormatting.RESET);
        content = Patterns.underlinePattern.matcher(content).replaceAll(TextFormatting.UNDERLINE + "$1" + TextFormatting.RESET);
        content = Patterns.lineThroughPattern.matcher(content).replaceAll("$1");
        content = Patterns.multiCodePattern.matcher(content).replaceAll("$1");
        content = Patterns.singleCodePattern.matcher(content).replaceAll("$1");

        content = EmojiParser.parseToAliases(content, EmojiParser.FitzpatrickAction.REMOVE);

        content = Utils.Replace(emoteMap, content);

        if (!this.customFormatting) {
            String[] messageParts = this.message
                    .replace("%MESSAGE%",
                            ForgeHooks.newChatWithLinks(
                                    content
                            ).getFormattedText()
                    )
                    .split("%USER%");

            ITextComponent usernameComponent = new TextComponentString(event.getAuthor().getUsername());

            HoverEvent usernameHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Mention user (Clears current message)"));
            ClickEvent usernameClickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "@" + event.getAuthor().getUsername() + " ");

            usernameComponent.getStyle()
                    .setColor(this.usernameColor)
                    .setClickEvent(usernameClickEvent)
                    .setHoverEvent(usernameHoverEvent);

            ITextComponent chatComponent = new TextComponentString(messageParts[0]);
            List<Message.Attachment> attachments = event.getMessage().getAttachments();

            for (int i = 1, j = messageParts.length; i < j; i++) {
                chatComponent.appendSibling(
                        usernameComponent
                ).appendText(
                        messageParts[i]
                );
            }

            for (Message.Attachment attachment : attachments) {
                chatComponent.appendText(" ");
                chatComponent.appendSibling(this.attachmentToTextComponent(attachment));
            }

            for (EntityPlayerMP player : players) {
                if (content.contains(player.getDisplayNameString())) {
                    String playerName = player.getDisplayNameString();

                    String[] playerMessageParts = this.message
                            .replace("%MESSAGE%",
                                    ForgeHooks.newChatWithLinks(
                                            content.replaceAll(
                                                    "\\b" + playerName + "\\b",
                                                    TextFormatting.BLUE + playerName + TextFormatting.RESET
                                            ), false
                                    ).getFormattedText()
                            )
                            .split("%USER%");

                    ITextComponent playerChatComponent = new TextComponentString(playerMessageParts[0]);
                    for (int i = 1, j = playerMessageParts.length; i < j; i++) {
                        playerChatComponent.appendSibling(
                                usernameComponent
                        ).appendText(
                                playerMessageParts[i]
                        );
                    }

                    for (Message.Attachment attachment : attachments) {
                        playerChatComponent.appendText(" ");
                        playerChatComponent.appendSibling(this.attachmentToTextComponent(attachment));
                    }

                    player.addChatMessage(playerChatComponent);
                    continue;
                }

                player.addChatMessage(chatComponent);
            }
        } else {
            String pattern = this.message;

            pattern = Patterns.customFormattingPattern.matcher(pattern).replaceAll("§$1");

            ITextComponent message = ForgeHooks.newChatWithLinks(
                    pattern
                            .replace("%MESSAGE%", content)
                            .replace("%USER%", event.getAuthorName())
            );

            List<Message.Attachment> attachments = event.getMessage().getAttachments();

            for (Message.Attachment attachment : attachments) {
                message.appendText(" ");
                message.appendSibling(this.attachmentToTextComponent(attachment));
            }

            for (EntityPlayerMP player : players) {
                player.addChatMessage(message);
            }
        }
    }

    @Override
    protected void readExtra(JsonReader reader, String name) throws IOException {
        switch (name) {
            case "maxLength":
                if (reader.peek() == JsonToken.NUMBER) {
                    this.maxChatLength = reader.nextInt();
                    return;
                }
                break;
            case "usernameColor":
                if (reader.peek() == JsonToken.STRING) {
                    this.usernameColor = TextFormatting.getValueByName(reader.nextString());
                    return;
                }
            case "customFormatting":
                if (reader.peek() == JsonToken.BOOLEAN) {
                    this.customFormatting = reader.nextBoolean();
                    return;
                }
        }

        reader.skipValue();
    }

    @Override
    protected void writeExtra(JsonWriter writer) throws IOException {
        writer.name("maxLength");
        writer.value(this.maxChatLength);
        writer.name("usernameColor");
        writer.value(this.usernameColor.getFriendlyName());
        writer.name("customFormatting");
        writer.value(this.customFormatting);
    }
}
