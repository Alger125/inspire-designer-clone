package com.vdp.core.view.command;

import java.util.Stack;

public class CommandManager {
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();
    private Runnable onStateChanged;

    public void setOnStateChanged(Runnable onStateChanged) {
        this.onStateChanged = onStateChanged;
    }

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        notifyStateChanged();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            notifyStateChanged();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
            notifyStateChanged();
        }
    }
    
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    
    public String getUndoName() { return canUndo() ? undoStack.peek().getName() : ""; }
    public String getRedoName() { return canRedo() ? redoStack.peek().getName() : ""; }
    
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyStateChanged();
    }

    private void notifyStateChanged() {
        if (onStateChanged != null) {
            onStateChanged.run();
        }
    }

    public int getUndoStackSize() { return undoStack.size(); }
}
