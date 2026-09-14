package com.vdp.core.view;

import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.InspireModule;
import com.vdp.core.model.Workflow;
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
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

final class WorkflowCanvas extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final class Wire {
        private final WorkflowNode source;
        private final WorkflowNode target;

        private Wire(WorkflowNode source, WorkflowNode target) {
            this.source = source;
            this.target = target;
        }
    }

    private final Workflow workflow;
    private final Consumer<WorkflowNode> moduleEditor;
    private final Consumer<String> statusWriter;
    private final List<Wire> wires = new ArrayList<>();
    private WorkflowNode connectingSource;
    private Point connectionCursor;

    WorkflowCanvas(
            Workflow workflow,
            Consumer<WorkflowNode> moduleEditor,
            Consumer<String> statusWriter) {
        this.workflow = workflow;
        this.moduleEditor = moduleEditor;
        this.statusWriter = statusWriter;
        setLayout(null);
        setBackground(InspireTheme.CANVAS);
        setPreferredSize(new Dimension(1100, 720));
        setTransferHandler(createDropHandler());
    }

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
                if (!canImport(support)) {
                    return false;
                }
                try {
                    String type = (String) support.getTransferable()
                            .getTransferData(DataFlavor.stringFlavor);
                    Point point = support.getDropLocation().getDropPoint();
                    addModule(type, point);
                    return true;
                } catch (Exception exception) {
                    statusWriter.accept("Could not add module: " + exception.getMessage());
                    return false;
                }
            }
        };
    }

    private void addModule(String type, Point dropPoint) {
        InspireModule module = createModel(type);
        Color accent = type.equals("Data Filter")
                ? InspireTheme.DATA_PROCESSING : InspireTheme.DATA_INPUT;
        boolean acceptsInput = type.equals("Data Filter");
        WorkflowNode node = new WorkflowNode(type, module, accent, acceptsInput);
        int x = Math.max(8, dropPoint.x - WorkflowNode.WIDTH / 2);
        int y = Math.max(62, dropPoint.y - WorkflowNode.HEIGHT / 2);
        node.setLocation(x, y);
        installNodeInteraction(node);
        workflow.addModule(module);
        add(node);
        setComponentZOrder(node, 0);
        statusWriter.accept(type + " added to workflow");
        repaint();
    }

    private InspireModule createModel(String type) {
        return switch (type) {
            case "Data Generator" -> new DataGeneratorModule();
            case "Data Input" -> new DataInputModule();
            case "Data Filter" -> new DataFilterModule();
            default -> throw new IllegalArgumentException("Module not implemented: " + type);
        };
    }

    private void installNodeInteraction(WorkflowNode node) {
        MouseAdapter interaction = new MouseAdapter() {
            private Point dragOffset;

            @Override
            public void mousePressed(MouseEvent event) {
                selectOnly(node);
                if (SwingUtilities.isRightMouseButton(event)) {
                    showNodeMenu(node, event.getX(), event.getY());
                    return;
                }
                if (node.isOutputHit(event.getPoint())) {
                    connectingSource = node;
                    connectionCursor = SwingUtilities.convertPoint(node, event.getPoint(), WorkflowCanvas.this);
                } else {
                    dragOffset = event.getPoint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (connectingSource == node) {
                    connectionCursor = SwingUtilities.convertPoint(node, event.getPoint(), WorkflowCanvas.this);
                } else if (dragOffset != null) {
                    Point canvasPoint = SwingUtilities.convertPoint(node, event.getPoint(), WorkflowCanvas.this);
                    node.setLocation(
                            Math.max(0, canvasPoint.x - dragOffset.x),
                            Math.max(55, canvasPoint.y - dragOffset.y));
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (connectingSource == node) {
                    Point canvasPoint = SwingUtilities.convertPoint(node, event.getPoint(), WorkflowCanvas.this);
                    finishConnection(node, canvasPoint);
                }
                dragOffset = null;
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
        if (target != null && wires.stream().noneMatch(
                wire -> wire.source == source && wire.target == target)) {
            wires.add(new Wire(source, target));
            statusWriter.accept(source.getModule().getName() + " connected to "
                    + target.getModule().getName());
        }
        connectingSource = null;
        connectionCursor = null;
        repaint();
    }

    private WorkflowNode findInputNode(Point point, WorkflowNode source) {
        for (Component component : getComponents()) {
            if (component instanceof WorkflowNode candidate
                    && candidate != source
                    && candidate.acceptsInput()
                    && candidate.getBounds().contains(point)) {
                return candidate;
            }
        }
        return null;
    }

    private void selectOnly(WorkflowNode selected) {
        for (Component component : getComponents()) {
            if (component instanceof WorkflowNode node) {
                node.setNodeSelected(node == selected);
            }
        }
    }

    private void showNodeMenu(WorkflowNode node, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Edit Module");
        edit.addActionListener(event -> moduleEditor.accept(node));
        menu.add(edit);
        menu.add(new JMenuItem("Choose Module Icon"));
        menu.add(new JMenuItem("Rename Module"));
        menu.addSeparator();
        JMenuItem delete = new JMenuItem("Delete Module");
        delete.addActionListener(event -> removeNode(node));
        menu.add(delete);
        menu.addSeparator();
        menu.add(new JMenuItem("Lock"));
        menu.add(new JMenuItem("Lock with Password"));
        menu.show(node, x, y);
    }

    private void removeNode(WorkflowNode node) {
        wires.removeIf(wire -> wire.source == node || wire.target == node);
        workflow.removeModule(node.getModule());
        remove(node);
        statusWriter.accept(node.getModule().getName() + " removed");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintWorkflowInfo(g2);
        paintFloatingTools(g2);

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(165, 165, 165));
        for (Wire wire : wires) {
            paintWire(g2, outputPoint(wire.source), inputPoint(wire.target));
        }
        if (connectingSource != null && connectionCursor != null) {
            g2.setColor(InspireTheme.LAYOUT);
            paintWire(g2, outputPoint(connectingSource), connectionCursor);
        }
        g2.dispose();
    }

    private void paintWorkflowInfo(Graphics2D g2) {
        g2.setColor(new Color(145, 145, 145));
        g2.setFont(getFont().deriveFont(Font.PLAIN, 27f));
        g2.drawString("New Workflow 1.wfd", 12, 31);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
        g2.drawString("Path: not saved", 12, 45);
        g2.drawString("Module count: " + workflow.getModules().size(), 12, 57);
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

    private Point outputPoint(WorkflowNode node) {
        Point local = node.outputPoint();
        return new Point(node.getX() + local.x, node.getY() + local.y);
    }

    private Point inputPoint(WorkflowNode node) {
        Point local = node.inputPoint();
        return new Point(node.getX() + local.x, node.getY() + local.y);
    }

    private void paintWire(Graphics2D g2, Point start, Point end) {
        int middleX = start.x + Math.max(20, (end.x - start.x) / 2);
        g2.drawLine(start.x, start.y, middleX, start.y);
        g2.drawLine(middleX, start.y, middleX, end.y);
        g2.drawLine(middleX, end.y, end.x, end.y);
    }
}
