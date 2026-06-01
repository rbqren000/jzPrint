package com.mx.mxSdk;

public class CommandContext {

    public Command command;
    public Command.Callback callback;

    public CommandContext(Command command, Command.Callback callback) {
        this.callback = callback;
        this.command = command;
    }

    public void clear() {
        this.command = null;
        this.callback = null;
    }
}
