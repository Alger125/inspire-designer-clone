package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Navigation and row mapping for Data Proof, with no Swing dependency. */
public final class DataProofModel {
    private final ExecutionSnapshot snapshot;
    private int currentRecordIndex;

    public DataProofModel(ExecutionSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        currentRecordIndex = snapshot.getRecordCount() == 0 ? -1 : 0;
    }

    public int getCurrentRecordIndex() { return currentRecordIndex; }
    public int getCurrentRecordNumber() { return currentRecordIndex < 0 ? 0 : currentRecordIndex + 1; }
    public int getTotalRecords() { return snapshot.getRecordCount(); }

    public void first() {
        if (getTotalRecords() > 0) currentRecordIndex = 0;
    }

    public void previous() {
        if (currentRecordIndex > 0) currentRecordIndex--;
    }

    public void next() {
        if (currentRecordIndex >= 0 && currentRecordIndex < getTotalRecords() - 1) {
            currentRecordIndex++;
        }
    }

    public void last() {
        if (getTotalRecords() > 0) currentRecordIndex = getTotalRecords() - 1;
    }

    public boolean selectRecordNumber(int oneBasedRecordNumber) {
        if (oneBasedRecordNumber < 1 || oneBasedRecordNumber > getTotalRecords()) {
            return false;
        }
        currentRecordIndex = oneBasedRecordNumber - 1;
        return true;
    }

    public List<DataProofRow> getRows() {
        List<DataProofRow> rows = new ArrayList<>();
        DataNode rootNode = snapshot.getRootNode();
        if (rootNode == null) {
            return List.copyOf(rows);
        }

        String position = getTotalRecords() == 0
                ? "0/0" : getCurrentRecordNumber() + "/" + getTotalRecords();
        
        rows.add(new DataProofRow(rootNode.getName(), rootNode.getType().name(), position, 0));

        if (currentRecordIndex >= 0 && rootNode.getType() == DataNode.NodeType.ARRAY) {
            if (currentRecordIndex < rootNode.getChildren().size()) {
                DataNode currentRecord = rootNode.getChildren().get(currentRecordIndex);
                flattenNode(currentRecord, rows, 1);
            }
        } else {
            flattenNode(rootNode, rows, 1);
        }

        return List.copyOf(rows);
    }

    private void flattenNode(DataNode node, List<DataProofRow> rows, int depth) {
        if (node.getType() == DataNode.NodeType.VALUE) {
            rows.add(new DataProofRow(node.getName(), "Value", node.getValue() != null ? node.getValue() : "", depth));
        } else {
            String value = node.getType() == DataNode.NodeType.ARRAY ? "[" + node.getChildren().size() + " items]" : "{...}";
            rows.add(new DataProofRow(node.getName(), node.getType().name(), value, depth));
            for (DataNode child : node.getChildren()) {
                flattenNode(child, rows, depth + 1);
            }
        }
    }
}
