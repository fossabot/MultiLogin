package moe.caa.multilogin.bungee.impl;

import moe.caa.multilogin.core.ISender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeSender implements ISender {
    private final CommandSender vanSender;

    public BungeeSender(CommandSender vanSender) {
        this.vanSender = vanSender;
    }

    @Override
    public String getSenderName() {
        return vanSender.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return vanSender.hasPermission(permission);
    }

    @Override
    public void sendMessage(BaseComponent text) {
        vanSender.sendMessage(text);
    }

    @Override
    public boolean isOp() {
        return false;
    }

    @Override
    public boolean isPlayer() {
        return vanSender instanceof ProxiedPlayer;
    }
}