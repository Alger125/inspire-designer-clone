package com.vdp.core.view.command;

public interface Command {
    void execute();
    void undo();
    String getName();
}
