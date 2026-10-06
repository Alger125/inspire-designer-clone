package com.vdp.core.view;

import com.vdp.core.model.BaseDataInputModule;
import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.DataConcatenatorModule;
import com.vdp.core.model.DataSorterModule;
import com.vdp.core.model.HttpJsonDataInputModule;
import com.vdp.core.model.InspireModule;
import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.model.WorkflowSerializer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vdp.core.model.WorkflowSerializer;
import java.util.function.Consumer;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import com.vdp.core.model.Port;

public final class WorkflowCanvas extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final double ZOOM_STEP = 0.25;
    private static final double ZOOM_MIN  = 0.25;
    private static final double ZOOM_MAX  = 2.0;

    // ── Fields ────────────────────────────────────────────────────────────────

    private Workflow workflow;
    private final Consumer<WorkflowNode> moduleEditor;
    private final Consumer<String>       statusWriter;
    /** Tracks the palette-type string for each module id so we can serialize. */
    private final Map<String, String>    moduleTypes = new LinkedHashMap<>();

    private WorkflowNode connectingSource;
    private Port connectingSourcePort;
    private final Set<WorkflowNode> selectedNodes = new LinkedHashSet<>();
    private WorkflowNode primarySelectedNode;
    private WorkflowConnection selectedConnection;
    private final com.vdp.core.view.command.CommandManager commandManager = new com.vdp.core.view.command.CommandManager();
    private java.util.Map<WorkflowNode, java.awt.Point> dragInitialPositions;
    private java.awt.Point dragStartCanvas;
    private java.awt.Rectangle selectionRect;
    private java.awt.Point rubberbandStart;
    private final Set<WorkflowNode> rubberbandInitialSelection = new java.util.LinkedHashSet<>();
    private Point        connectionCursor;
    
    private static final class ClipboardItem {
        final com.fasterxml.jackson.databind.node.ObjectNode config;
        final String type;
        final String originalId;
        final java.awt.Point position;
        ClipboardItem(com.fasterxml.jackson.databind.node.ObjectNode config, String type, String originalId, java.awt.Point position) {
            this.config = config;
            this.type = type;
            this.originalId = originalId;
            this.position = position;
        }
    }
    private final java.util.List<ClipboardItem> clipboardItems = new java.util.ArrayList<>();
    private final java.util.List<com.vdp.core.model.WorkflowConnection> clipboardConnections = new java.util.ArrayList<>();
    private int pasteOffsetMultiplier = 0;
    private double       zoomFactor = 1.0;

    // ── Constructor ───────────────────────────────────────────────────────────

    WorkflowCanvas(Consumer<WorkflowNode> moduleEditor, Consumer<String> statusWriter) {
        this.workflow     = new Workflow("New Workflow 1");
        this.moduleEditor = moduleEditor;
        this.statusWriter = statusWriter;
        
        setFocusable(true);
        getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("DELETE"), "deleteNode");
        getActionMap().put("deleteNode", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!selectedNodes.isEmpty()) {
                    commandManager.executeCommand(new com.vdp.core.view.command.DeleteNodesCommand(WorkflowCanvas.this, new java.util.ArrayList<>(selectedNodes), WorkflowCanvas.this::repaint));
                    selectedNodes.clear();
                    primarySelectedNode = null;
                    statusWriter.accept("Selected modules deleted");
                } else if (selectedConnection != null) {
                    commandManager.executeCommand(new com.vdp.core.view.command.DeleteConnectionCommand(WorkflowCanvas.this, selectedConnection, WorkflowCanvas.this::repaint));
                    selectedConnection = null;
                    statusWriter.accept("Connection deleted");
                }
            }
        });
        
        getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK), "copyNodes");
        getActionMap().put("copyNodes", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                copySelectedNodes();
            }
        });
        
        getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK), "pasteNodes");
        getActionMap().put("pasteNodes", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pasteClipboardNodes();
            }
        });
        
        getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK), "undoCommand");
        getActionMap().put("undoCommand", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                commandManager.undo();
            }
        });
        
        getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_DOWN_MASK), "redoCommand");
        getActionMap().put("redoCommand", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                commandManager.redo();
            }
        });
        setLayout(null);
        setBackground(InspireTheme.CANVAS);
        setPreferredSize(new Dimension(1100, 720));
        setTransferHandler(createDropHandler());
        installCanvasInteraction();
    }
    
    private void installCanvasInteraction() {
        java.awt.event.MouseAdapter ma = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    selectedConnection = findConnectionAt(e.getPoint());
                    if (selectedConnection != null) {
                        selectOnly(null);
                        repaint();
                    } else {
                        if (!e.isControlDown()) {
                            selectOnly(null);
                        }
                        rubberbandStart = e.getPoint();
                        selectionRect = new java.awt.Rectangle(rubberbandStart);
                        rubberbandInitialSelection.clear();
                        if (e.isControlDown()) {
                            rubberbandInitialSelection.addAll(selectedNodes);
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    selectedConnection = findConnectionAt(e.getPoint());
                    if (selectedConnection != null) {
                        selectOnly(null);
                        showConnectionMenu(e.getX(), e.getY());
                    }
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (rubberbandStart != null) {
                    int x = Math.min(rubberbandStart.x, e.getX());
                    int y = Math.min(rubberbandStart.y, e.getY());
                    int width = Math.abs(rubberbandStart.x - e.getX());
                    int height = Math.abs(rubberbandStart.y - e.getY());
                    selectionRect = new java.awt.Rectangle(x, y, width, height);
                    
                    selectedNodes.clear();
                    selectedNodes.addAll(rubberbandInitialSelection);
                    for (java.awt.Component comp : getComponents()) {
                        if (comp instanceof WorkflowNode node) {
                            if (selectionRect.intersects(node.getBounds())) {
                                selectedNodes.add(node);
                            }
                        }
                    }
                    updateNodesSelectionState();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (rubberbandStart != null) {
                    rubberbandStart = null;
                    selectionRect = null;
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }
    
    private WorkflowConnection findConnectionAt(Point p) {
        for (WorkflowConnection conn : workflow.getConnections()) {
            WorkflowNode src = findNodeById(conn.sourceModuleId());
            WorkflowNode tgt = findNodeById(conn.targetModuleId());
            if (src != null && tgt != null) {
                Point start = getPortPoint(src, conn.sourcePortId(), false);
                Point end = getPortPoint(tgt, conn.targetPortId(), true);
                int midX = start.x + Math.max(20, (end.x - start.x) / 2);
                
                // Check distance to the three segments of the wire
                if (isPointNearLineSegment(p, start.x, start.y, midX, start.y, 5) ||
                    isPointNearLineSegment(p, midX, start.y, midX, end.y, 5) ||
                    isPointNearLineSegment(p, midX, end.y, end.x, end.y, 5)) {
                    return conn;
                }
            }
        }
        return null;
    }
    
    private boolean isPointNearLineSegment(Point p, int x1, int y1, int x2, int y2, int tolerance) {
        double dist;
        if (x1 == x2) {
            // Vertical line
            if (p.y >= Math.min(y1, y2) - tolerance && p.y <= Math.max(y1, y2) + tolerance) {
                dist = Math.abs(p.x - x1);
            } else {
                return false;
            }
        } else {
            // Horizontal line
            if (p.x >= Math.min(x1, x2) - tolerance && p.x <= Math.max(x1, x2) + tolerance) {
                dist = Math.abs(p.y - y1);
            } else {
                return false;
            }
        }
        return dist <= tolerance;
    }
    
    private void showConnectionMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem delete = new JMenuItem("Delete Connection");
        delete.addActionListener(event -> {
            if (selectedConnection != null) {
                commandManager.executeCommand(new com.vdp.core.view.command.DeleteConnectionCommand(WorkflowCanvas.this, selectedConnection, WorkflowCanvas.this::repaint));
                selectedConnection = null;
                statusWriter.accept("Connection deleted");
            }
        });
        menu.add(delete);
        menu.show(this, x, y);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    // -- Clipboard --

    void copySelectedNodes() {
        clipboardItems.clear();
        clipboardConnections.clear();
        pasteOffsetMultiplier = 0;
        if (selectedNodes.isEmpty()) return;
        
        HashSet<String> selectedIds = new HashSet<>();
        for (WorkflowNode node : selectedNodes) {
            String type = moduleTypes.get(node.getModule().getId());
            ObjectNode config = WorkflowSerializer.serializeConfig(node.getModule());
            clipboardItems.add(new ClipboardItem(config, type, node.getModule().getId(), new java.awt.Point(node.getLocation())));
            selectedIds.add(node.getModule().getId());
        }
        
        for (WorkflowConnection conn : workflow.getConnections()) {
            if (selectedIds.contains(conn.sourceModuleId()) && selectedIds.contains(conn.targetModuleId())) {
                clipboardConnections.add(conn);
            }
        }
        statusWriter.accept("Copied " + clipboardItems.size() + " modules");
    }

    void pasteClipboardNodes() {
        if (clipboardItems.isEmpty()) return;
        
        pasteOffsetMultiplier++;
        int offset = pasteOffsetMultiplier * 30;
        
        HashMap<String, String> oldIdToNewId = new HashMap<>();
        ArrayList<WorkflowNode> newNodes = new ArrayList<>();
        
        for (ClipboardItem item : clipboardItems) {
            com.vdp.core.model.InspireModule newModule = WorkflowSerializer.createModule(item.type, item.config);
            oldIdToNewId.put(item.originalId, newModule.getId());
            
            java.awt.Color accent = isProcessingModule(item.type) ? InspireTheme.DATA_PROCESSING : InspireTheme.DATA_INPUT;
            boolean acceptsInput = isProcessingModule(item.type);
            WorkflowNode newNode = new WorkflowNode(item.type, newModule, accent, acceptsInput);
            installNodeInteraction(newNode);
            workflow.addModule(newModule);
            moduleTypes.put(newModule.getId(), item.type);
            
            newNode.setLocation(item.position.x + offset, item.position.y + offset);
            add(newNode);
            setComponentZOrder(newNode, 0);
            newNodes.add(newNode);
        }
        
        for (WorkflowConnection conn : clipboardConnections) {
            String newSourceId = oldIdToNewId.get(conn.sourceModuleId());
            String newTargetId = oldIdToNewId.get(conn.targetModuleId());
            if (newSourceId != null && newTargetId != null) {
                com.vdp.core.model.InspireModule sourceModule = workflow.findModule(newSourceId);
                com.vdp.core.model.InspireModule targetModule = workflow.findModule(newTargetId);
                if (sourceModule != null && targetModule != null) {
                    com.vdp.core.model.Port sPort = sourceModule.getOutputPorts().stream().filter(p -> p.getId().equals(conn.sourcePortId())).findFirst().orElse(null);
                    com.vdp.core.model.Port tPort = targetModule.getInputPorts().stream().filter(p -> p.getId().equals(conn.targetPortId())).findFirst().orElse(null);
                    if (sPort != null && tPort != null) {
                        workflow.connect(sourceModule, sPort, targetModule, tPort);
                    }
                }
            }
        }
        
        selectOnly(null);
        for (WorkflowNode newNode : newNodes) {
            selectedNodes.add(newNode);
        }
        if (!newNodes.isEmpty()) {
            primarySelectedNode = newNodes.get(0);
        }
        updateNodesSelectionState();
        
        revalidate();
        repaint();
        statusWriter.accept("Pasted " + newNodes.size() + " modules");
    }

    /** Returns the live workflow model. */
    public Workflow getWorkflow() { return workflow; }

    /** Returns which modules are selected (used by runProof). */
    String getSelectedModuleId() {
        return primarySelectedNode == null ? null : primarySelectedNode.getModule().getId();
    }

    /** Returns a snapshot of palette-type strings by module id. */
    public Map<String, String> getModuleTypes() { return Map.copyOf(moduleTypes); }
    void registerModuleType(String moduleId, String type) { moduleTypes.put(moduleId, type); }

    /** Returns the current canvas position of every node. */
    Map<String, Point> getNodePositions() {
        Map<String, Point> positions = new LinkedHashMap<>();
        for (Component comp : getComponents()) {
            if (comp instanceof WorkflowNode node) {
                positions.put(node.getModule().getId(),
                        new Point(node.getX(), node.getY()));
            }
        }
        return positions;
    }

    // ── File operations (called by MainAppWindow) ─────────────────────────────

    /** Clears the canvas and starts a fresh empty workflow. */
    void newWorkflow(String name) {
        clearCanvas();
        workflow = new Workflow(name);
        repaint();
    }

    /**
     * Rebuilds the canvas from a {@link WorkflowSerializer.LoadResult}.
     * Called by MainAppWindow after a successful file open.
     */
    void loadFrom(WorkflowSerializer.LoadResult result) {
        clearCanvas();
        workflow = result.workflow();
        moduleTypes.putAll(result.moduleTypes());

        for (InspireModule module : workflow.getModules()) {
            String type      = moduleTypes.getOrDefault(module.getId(), "Unknown");
            Point  pos       = result.nodePositions().getOrDefault(module.getId(), new Point(100, 100));
            Color  accent    = isProcessingModule(type)
                    ? InspireTheme.DATA_PROCESSING : InspireTheme.DATA_INPUT;
            boolean acceptsInput = isProcessingModule(type);

            WorkflowNode node = new WorkflowNode(type, module, accent, acceptsInput);
            node.setLocation(pos.x, pos.y);
            installNodeInteraction(node);
            add(node);
            setComponentZOrder(node, 0);
        }

        revalidate();
        repaint();
    }

    // ── Zoom ──────────────────────────────────────────────────────────────────

    void zoomIn()  { applyZoom(Math.min(ZOOM_MAX, zoomFactor + ZOOM_STEP)); }
    void zoomOut() { applyZoom(Math.max(ZOOM_MIN, zoomFactor - ZOOM_STEP)); }

    private void applyZoom(double newZoom) {
        if (newZoom == zoomFactor) return;
        double ratio = newZoom / zoomFactor;
        zoomFactor   = newZoom;

        for (Component comp : getComponents()) {
            if (comp instanceof WorkflowNode node) {
                int nx = (int) Math.round(node.getX() * ratio);
                int ny = (int) Math.round(node.getY() * ratio);
                node.setLocation(nx, ny);
            }
        }
        setPreferredSize(new Dimension(
                (int) (1100 * zoomFactor), (int) (720 * zoomFactor)));
        revalidate();
        repaint();
        statusWriter.accept(String.format("Zoom: %.0f%%", zoomFactor * 100));
    }

    // ── Drag-and-Drop handler ─────────────────────────────────────────────────

    private TransferHandler createDropHandler() {
        return new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop()
                        && support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    String type = (String) support.getTransferable()
                            .getTransferData(DataFlavor.stringFlavor);
                    addModule(type, support.getDropLocation().getDropPoint());
                    return true;
                } catch (Exception exception) {
                    statusWriter.accept("Could not add module: " + exception.getMessage());
                    return false;
                }
            }
        };
    }

    // ── Module factory ────────────────────────────────────────────────────────

    private void addModule(String type, Point dropPoint) {
        InspireModule module      = createModel(type);
        Color         accent      = isProcessingModule(type)
                ? InspireTheme.DATA_PROCESSING : InspireTheme.DATA_INPUT;
        boolean       acceptsInput = isProcessingModule(type);

        WorkflowNode node = new WorkflowNode(type, module, accent, acceptsInput);
        int x = Math.max(8,  dropPoint.x - WorkflowNode.WIDTH  / 2);
        int y = Math.max(62, dropPoint.y - WorkflowNode.HEIGHT / 2);
        node.setLocation(x, y);

        installNodeInteraction(node);
        workflow.addModule(module);
        moduleTypes.put(module.getId(), type);
        add(node);
        setComponentZOrder(node, 0);
        statusWriter.accept(type + " added to workflow");
        repaint();
    }

    private InspireModule createModel(String type) {
        return switch (type) {
            case "Data Generator"  -> new DataGeneratorModule();
            case "Data Input"      -> new DataInputModule();
            case "HTTP JSON Input" -> new HttpJsonDataInputModule();
            case "Data Filter"     -> new DataFilterModule();
            case "Data Sorter"     -> new DataSorterModule();
            case "Data Transformer" -> new com.vdp.core.model.DataTransformerModule();
            case "Data Concatenator" -> new DataConcatenatorModule();
            default -> throw new IllegalArgumentException("Module not implemented: " + type);
        };
    }

    private boolean isProcessingModule(String type) {
        return type.equals("Data Filter") || type.equals("Data Sorter")
                || type.equals("Data Transformer") || type.equals("Data Concatenator");
    }

    // ── Node interaction ──────────────────────────────────────────────────────

    private void installNodeInteraction(WorkflowNode node) {
        MouseAdapter interaction = new MouseAdapter() {


            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    if (!selectedNodes.contains(node)) {
                        selectOnly(node);
                    }
                    showNodeMenu(node, event.getX(), event.getY());
                    return;
                }
                if (event.isControlDown()) {
                    toggleSelection(node);
                } else if (!selectedNodes.contains(node)) {
                    selectOnly(node);
                }
                
                Port hitPort = node.getOutputPortHit(event.getPoint());
                if (hitPort != null) {
                    connectingSource = node;
                    connectingSourcePort = hitPort;
                    connectionCursor = SwingUtilities.convertPoint(
                            node, event.getPoint(), WorkflowCanvas.this);
                } else {
                    if (selectedNodes.contains(node)) {
                        dragStartCanvas = SwingUtilities.convertPoint(node, event.getPoint(), WorkflowCanvas.this);
                        dragInitialPositions = new java.util.HashMap<>();
                        for (WorkflowNode n : selectedNodes) {
                            dragInitialPositions.put(n, n.getLocation());
                        }
                    } else {
                        dragStartCanvas = null;
                        dragInitialPositions = null;
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (connectingSource == node) {
                    connectionCursor = SwingUtilities.convertPoint(
                            node, event.getPoint(), WorkflowCanvas.this);
                } else if (dragStartCanvas != null && dragInitialPositions != null) {
                    Point currentCanvas = SwingUtilities.convertPoint(
                            node, event.getPoint(), WorkflowCanvas.this);
                    int dx = currentCanvas.x - dragStartCanvas.x;
                    int dy = currentCanvas.y - dragStartCanvas.y;
                    
                    int minAllowedDx = Integer.MIN_VALUE;
                    int minAllowedDy = Integer.MIN_VALUE;
                    for (java.util.Map.Entry<WorkflowNode, Point> entry : dragInitialPositions.entrySet()) {
                        int allowedDx = 0 - entry.getValue().x;
                        int allowedDy = 55 - entry.getValue().y;
                        if (allowedDx > minAllowedDx) minAllowedDx = allowedDx;
                        if (allowedDy > minAllowedDy) minAllowedDy = allowedDy;
                    }
                    
                    int finalDx = Math.max(dx, minAllowedDx);
                    int finalDy = Math.max(dy, minAllowedDy);
                    
                    for (java.util.Map.Entry<WorkflowNode, Point> entry : dragInitialPositions.entrySet()) {
                        entry.getKey().setLocation(entry.getValue().x + finalDx, entry.getValue().y + finalDy);
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (connectingSource == node) {
                    finishConnection(node,
                            SwingUtilities.convertPoint(
                                    node, event.getPoint(), WorkflowCanvas.this));
                } else if (dragStartCanvas != null && dragInitialPositions != null) {
                    java.util.Map<WorkflowNode, Point> finalPositions = new java.util.HashMap<>();
                    for (WorkflowNode n : selectedNodes) {
                        finalPositions.put(n, n.getLocation());
                    }
                    if (!finalPositions.equals(dragInitialPositions)) {
                        commandManager.executeCommand(new com.vdp.core.view.command.MoveNodesCommand(dragInitialPositions, finalPositions, WorkflowCanvas.this::repaint));
                    }
                }
                dragStartCanvas = null;
                dragInitialPositions = null;
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    moduleEditor.accept(node);
                }
            }
        };
        node.addMouseListener(interaction);
        node.addMouseMotionListener(interaction);
    }

    private void finishConnection(WorkflowNode source, Point point) {
        WorkflowNode target = findInputNode(point, source);

        if (target == null) {
            connectingSource = null;
            connectingSourcePort = null;
            connectionCursor = null;
            repaint();
            return;
        }

        Point targetPoint = SwingUtilities.convertPoint(this, point, target);
        Port targetPort = target.getInputPortHit(targetPoint);
        
        // Enforce exact port hit instead of falling back


        if (targetPort != null && connectingSourcePort != null) {
            final Port tp = targetPort; // effective final for lambda
            boolean alreadyConnected = workflow.getConnections().stream().anyMatch(
                    c -> c.sourceModuleId().equals(source.getModule().getId()) && 
                         c.targetModuleId().equals(target.getModule().getId()) &&
                         c.targetPortId().equals(tp.getId()));

            if (!alreadyConnected) {
                try {
                    workflow.connect(
                            source.getModule(),
                            connectingSourcePort,
                            target.getModule(),
                            targetPort
                    );
                    statusWriter.accept(source.getModule().getName()
                            + " connected to " + target.getModule().getName());
                } catch (IllegalArgumentException exception) {
                    statusWriter.accept("Connection rejected: " + exception.getMessage());
                }
            }
        }

        connectingSource = null;
        connectingSourcePort = null;
        connectionCursor = null;
        repaint();
    }

    private WorkflowNode findInputNode(Point point, WorkflowNode source) {
        for (Component comp : getComponents()) {
            if (comp instanceof WorkflowNode candidate
                    && candidate != source
                    && candidate.acceptsInput()
                    && candidate.getBounds().contains(point)) {
                return candidate;
            }
        }
        return null;
    }

    private WorkflowNode findNodeById(String moduleId) {
        for (Component comp : getComponents()) {
            if (comp instanceof WorkflowNode node
                    && node.getModule().getId().equals(moduleId)) {
                return node;
            }
        }
        return null;
    }

    void selectOnly(WorkflowNode selected) {
        selectedNodes.clear();
        primarySelectedNode = selected;
        if (selected != null) {
            selectedNodes.add(selected);
        }
        selectedConnection = null;
        updateNodesSelectionState();
    }

    void toggleSelection(WorkflowNode node) {
        if (selectedNodes.contains(node)) {
            selectedNodes.remove(node);
            if (primarySelectedNode == node) {
                primarySelectedNode = selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
            }
        } else {
            selectedNodes.add(node);
            primarySelectedNode = node;
        }
        selectedConnection = null;
        updateNodesSelectionState();
    }

    private void clearSelection() {
        selectedNodes.clear();
        primarySelectedNode = null;
        selectedConnection = null;
        updateNodesSelectionState();
    }

    private void updateNodesSelectionState() {
        if (primarySelectedNode == null || !selectedNodes.contains(primarySelectedNode)) {
            primarySelectedNode = selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
        }
        for (Component comp : getComponents()) {
            if (comp instanceof WorkflowNode node) {
                node.setNodeSelected(selectedNodes.contains(node));
            }
        }
        repaint();
    }

    // ── Context menu ──────────────────────────────────────────────────────────

    private void showNodeMenu(WorkflowNode node, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem edit = new JMenuItem("Edit Module");
        edit.addActionListener(event -> moduleEditor.accept(node));
        menu.add(edit);

        menu.add(new JMenuItem("Choose Module Icon"));   // placeholder

        JMenuItem rename = new JMenuItem("Rename Module");
        rename.addActionListener(event -> renameNode(node));
        menu.add(rename);

        menu.addSeparator();

        JMenuItem delete = new JMenuItem(selectedNodes.size() > 1 ? "Delete Selected Modules" : "Delete Module");
        delete.addActionListener(event -> {
            commandManager.executeCommand(new com.vdp.core.view.command.DeleteNodesCommand(WorkflowCanvas.this, new java.util.ArrayList<>(selectedNodes), WorkflowCanvas.this::repaint));
            selectedNodes.clear();
            primarySelectedNode = null;
        });
        menu.add(delete);

        menu.addSeparator();
        menu.add(new JMenuItem("Lock"));                 // placeholder
        menu.add(new JMenuItem("Lock with Password"));   // placeholder

        menu.show(node, x, y);
    }

    /** Shows an input dialog and renames the module if the user confirms. */
    private void renameNode(WorkflowNode node) {
        String current = node.getModule().getName();
        String newName = (String) JOptionPane.showInputDialog(
                this,
                "New name for the module:",
                "Rename Module",
                JOptionPane.PLAIN_MESSAGE,
                null, null,
                current);
        if (newName == null || newName.isBlank()) return;
        newName = newName.trim();
        try {
            applyModuleName(node.getModule(), newName);
            node.repaint();
            statusWriter.accept("Renamed to \"" + newName + "\"");
        } catch (IllegalArgumentException exception) {
            statusWriter.accept("Could not rename: " + exception.getMessage());
        }
    }

    /**
     * Applies a new name to any concrete module type that exposes setName().
     * All current types support renaming; new ones can be added here.
     */
    private void applyModuleName(InspireModule module, String name) {
        if (module instanceof BaseDataInputModule m) {
            m.setName(name);
        } else if (module instanceof DataFilterModule m) {
            m.setName(name);
        } else if (module instanceof DataSorterModule m) {
            m.setName(name);
        } else if (module instanceof com.vdp.core.model.DataTransformerModule m) {
            m.setName(name);
        }
    }

    public void removeNodeDirectly(WorkflowNode node) {
        selectedNodes.remove(node);
        if (primarySelectedNode == node) {
            primarySelectedNode = selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
        }
        moduleTypes.remove(node.getModule().getId());
        workflow.removeModule(node.getModule());
        remove(node);
        statusWriter.accept(node.getModule().getName() + " removed");
    }
    
    public void addNodeDirectly(WorkflowNode node, String type) {
        workflow.addModule(node.getModule());
        moduleTypes.put(node.getModule().getId(), type);
        add(node);
        setComponentZOrder(node, 0);
    }
    
    private void removeNode(WorkflowNode node) {
        selectedNodes.remove(node);
        if (primarySelectedNode == node) {
            primarySelectedNode = selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
        }
        moduleTypes.remove(node.getModule().getId());
        workflow.removeModule(node.getModule());
        remove(node);
        statusWriter.accept(node.getModule().getName() + " removed");
        repaint();
    }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintWorkflowInfo(g2);
        paintFloatingTools(g2);

        g2.setStroke(new BasicStroke(2f));
        for (WorkflowConnection conn : workflow.getConnections()) {
            WorkflowNode src = findNodeById(conn.sourceModuleId());
            WorkflowNode tgt = findNodeById(conn.targetModuleId());
            if (src != null && tgt != null) {
                if (conn == selectedConnection) {
                    g2.setColor(InspireTheme.LAYOUT); // or InspireTheme.DATA_INPUT
                } else {
                    g2.setColor(new Color(165, 165, 165));
                }
                Point p1 = getPortPoint(src, conn.sourcePortId(), false);
                Point p2 = getPortPoint(tgt, conn.targetPortId(), true);
                paintWire(g2, p1, p2);
            }
        }
        if (connectingSource != null && connectionCursor != null) {
            g2.setColor(InspireTheme.LAYOUT);
            Point p1 = getPortPoint(connectingSource, connectingSourcePort.getId(), false);
            paintWire(g2, p1, connectionCursor);
        }
        if (selectionRect != null) {
            g2.setColor(new Color(0, 120, 215, 50));
            g2.fill(selectionRect);
            g2.setColor(new Color(0, 120, 215));
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.draw(selectionRect);
        }
        g2.dispose();
    }

    private Point getPortPoint(WorkflowNode node, String portId, boolean isInput) {
        java.util.List<Port> ports = isInput ? node.getModule().getInputPorts() : node.getModule().getOutputPorts();
        for (Port p : ports) {
            if (p.getId().equals(portId)) {
                Point pt = isInput ? node.inputPoint(p) : node.outputPoint(p);
                return SwingUtilities.convertPoint(node, pt, this);
            }
        }
        Point pt = isInput ? node.inputPoint() : node.outputPoint();
        return SwingUtilities.convertPoint(node, pt, this);
    }

    private void paintWorkflowInfo(Graphics2D g2) {
        g2.setColor(new Color(145, 145, 145));
        g2.setFont(getFont().deriveFont(Font.PLAIN, 27f));
        g2.drawString(workflow.getName() + ".wfd", 12, 31);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
        g2.drawString("Modules: " + workflow.getModules().size(), 12, 45);
        g2.drawString(String.format("Zoom: %.0f%%", zoomFactor * 100), 12, 57);
    }

    private void paintFloatingTools(Graphics2D g2) {
        int right = Math.max(620, getWidth() - 175);
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRect(right, 15, 65, 72);
        g2.fillRect(right + 74, 15, 88, 72);
        g2.setColor(InspireTheme.BORDER);
        g2.drawRect(right, 15, 65, 72);
        g2.drawRect(right + 74, 15, 88, 72);
        g2.setColor(InspireTheme.TEXT);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
        g2.drawString("Zoom", right + 7, 29);
        g2.drawString("Alignment", right + 81, 29);
        g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g2.drawString("+  −", right + 12, 56);
        g2.drawString("↤  ↔  ↦", right + 81, 56);
    }



    private void paintWire(Graphics2D g2, Point start, Point end) {
        int midX = start.x + Math.max(20, (end.x - start.x) / 2);
        g2.drawLine(start.x, start.y, midX,    start.y);
        g2.drawLine(midX,    start.y, midX,    end.y);
        g2.drawLine(midX,    end.y,   end.x,   end.y);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Removes all child components and resets internal state (but NOT workflow). */
    private void clearCanvas() {
        removeAll();
        moduleTypes.clear();
        selectedNodes.clear();
        primarySelectedNode = null;
        selectedConnection = null;
        selectionRect = null;
        rubberbandStart = null;
        rubberbandInitialSelection.clear();
        connectingSource = null;
        connectionCursor = null;
        zoomFactor      = 1.0;
        setPreferredSize(new Dimension(1100, 720));
        revalidate();
    }
}
