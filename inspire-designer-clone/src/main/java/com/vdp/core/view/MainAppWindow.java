package com.vdp.core.view;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

public class MainAppWindow extends JFrame {

    // --- 1. CLASES PARA LAS CONEXIONES (ENTREGA E4) ---
    class Wire {
        JInternalFrame source;
        JInternalFrame target;
        public Wire(JInternalFrame s, JInternalFrame t) { this.source = s; this.target = t; }
    }

    class WorkflowDesktopPane extends JDesktopPane {
        List<Wire> wires = new ArrayList<>();
        JInternalFrame draggingSource = null;
        Point dragPoint = null;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(3)); // Grosor de la línea
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Dibujar conexiones establecidas (Líneas grises)
            g2.setColor(new Color(150, 150, 150));
            for (Wire wire : wires) {
                Point p1 = getRightPortCoord(wire.source);
                Point p2 = getLeftPortCoord(wire.target);
                drawBezierCurve(g2, p1.x, p1.y, p2.x, p2.y);
            }

            // Dibujar la línea temporal mientras el usuario arrastra (Línea azul)
            if (draggingSource != null && dragPoint != null) {
                g2.setColor(new Color(52, 152, 219)); 
                Point p1 = getRightPortCoord(draggingSource);
                drawBezierCurve(g2, p1.x, p1.y, dragPoint.x, dragPoint.y);
            }
        }

        private Point getRightPortCoord(JInternalFrame f) {
            return new Point(f.getX() + f.getWidth(), f.getY() + f.getHeight() / 2);
        }

        private Point getLeftPortCoord(JInternalFrame f) {
            return new Point(f.getX(), f.getY() + f.getHeight() / 2);
        }

        private void drawBezierCurve(Graphics2D g2, int x1, int y1, int x2, int y2) {
            // Matemáticas para curvas suaves estilo Inspire Designer
            int ctrlx1 = x1 + 80;
            int ctrlx2 = x2 - 80;
            g2.draw(new java.awt.geom.CubicCurve2D.Double(x1, y1, ctrlx1, y1, ctrlx2, y2, x2, y2));
        }
    }

    private WorkflowDesktopPane workflowArea; 

    public MainAppWindow() {
        setTitle("Inspire Designer 14.0 Clone - Workflow Environment");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu("File"));
        menuBar.add(new JMenu("Edit"));
        menuBar.add(new JMenu("Workflow"));
        menuBar.add(new JMenu("Window"));
        menuBar.add(new JMenu("Help"));
        setJMenuBar(menuBar);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Modules");
        DefaultMutableTreeNode dataInputs = new DefaultMutableTreeNode("Data Inputs");
        dataInputs.add(new DefaultMutableTreeNode("Data Input"));
        DefaultMutableTreeNode dataProcessing = new DefaultMutableTreeNode("Data Processing");
        dataProcessing.add(new DefaultMutableTreeNode("Data Filter"));
        root.add(dataInputs);
        root.add(dataProcessing);

        JTree moduleTree = new JTree(new DefaultTreeModel(root));
        moduleTree.setDragEnabled(true);
        moduleTree.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) { return COPY; }
            @Override
            protected Transferable createTransferable(JComponent c) {
                JTree tree = (JTree) c;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node != null && node.isLeaf()) return new StringSelection(node.getUserObject().toString());
                return null;
            }
        });

        JScrollPane treeScrollPane = new JScrollPane(moduleTree);
        treeScrollPane.setMinimumSize(new Dimension(250, 0));

        // Usamos nuestro lienzo personalizado
        workflowArea = new WorkflowDesktopPane();
        workflowArea.setBackground(new Color(240, 240, 240));

        workflowArea.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) { return support.isDataFlavorSupported(DataFlavor.stringFlavor); }
            @Override
            public boolean importData(TransferSupport support) {
                try {
                    String nombreModulo = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    Point dropLocation = support.getDropLocation().getDropPoint();
                    crearModuloVisual(nombreModulo, dropLocation.x, dropLocation.y);
                    return true;
                } catch (Exception e) { e.printStackTrace(); }
                return false;
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, workflowArea);
        splitPane.setDividerLocation(250);
        getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    // --- 2. LÓGICA PARA CREAR MÓDULOS CON PUERTOS CONECTABLES ---
    private void crearModuloVisual(String nombreModulo, int x, int y) {
        if (nombreModulo.equals("Data Input") || nombreModulo.equals("Data Filter")) {
            JInternalFrame moduloUI = new JInternalFrame(nombreModulo, true, true, true, true);
            moduloUI.setSize(180, 100);
            moduloUI.setLocation(x, y);
            
            JPanel panelPrincipal = new JPanel(new BorderLayout());
            panelPrincipal.add(new JLabel("⚙️ " + nombreModulo, SwingConstants.CENTER), BorderLayout.CENTER);
            
            // Puerto Izquierdo (Entrada - Verde)
            JPanel puertoEntrada = new JPanel();
            puertoEntrada.setBackground(new Color(46, 204, 113)); 
            puertoEntrada.setPreferredSize(new Dimension(15, 0));
            puertoEntrada.setToolTipText("Puerto de Entrada (Input)");

            // Puerto Derecho (Salida - Azul)
            JPanel puertoSalida = new JPanel();
            puertoSalida.setBackground(new Color(52, 152, 219)); 
            puertoSalida.setPreferredSize(new Dimension(15, 0));
            puertoSalida.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            puertoSalida.setToolTipText("Arrastra desde aquí para conectar");

            // Eventos del ratón para dibujar la línea
            puertoSalida.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    workflowArea.draggingSource = moduloUI; // Inicia el dibujo
                }
                public void mouseReleased(MouseEvent e) {
                    // Calculamos dónde soltó el clic
                    Point dropPoint = SwingUtilities.convertPoint(puertoSalida, e.getPoint(), workflowArea);
                    JInternalFrame target = null;
                    
                    // Buscamos si soltó el ratón encima de otra cajita
                    for (Component c : workflowArea.getComponents()) {
                        if (c instanceof JInternalFrame && c != moduloUI) {
                            if (c.getBounds().contains(dropPoint)) {
                                target = (JInternalFrame) c;
                                break;
                            }
                        }
                    }
                    // Si encontró otra cajita, crea la conexión!
                    if (target != null) {
                        workflowArea.wires.add(new Wire(moduloUI, target));
                        System.out.println("🔗 ¡Conectados: " + moduloUI.getTitle() + " -> " + target.getTitle() + "!");
                    }
                    
                    // Reseteamos y repintamos
                    workflowArea.draggingSource = null;
                    workflowArea.dragPoint = null;
                    workflowArea.repaint();
                }
            });

            // Actualiza la línea azul mientras mueves el ratón
            puertoSalida.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    workflowArea.dragPoint = SwingUtilities.convertPoint(puertoSalida, e.getPoint(), workflowArea);
                    workflowArea.repaint();
                }
            });
            
            panelPrincipal.add(puertoEntrada, BorderLayout.WEST);
            panelPrincipal.add(puertoSalida, BorderLayout.EAST);
            moduloUI.add(panelPrincipal);
            
            // Si mueves la ventana, las líneas se actualizan solas
            moduloUI.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentMoved(java.awt.event.ComponentEvent e) {
                    workflowArea.repaint();
                }
            });

            moduloUI.setVisible(true);
            workflowArea.add(moduloUI); 
            try { moduloUI.setSelected(true); } catch (Exception e) {}
        }
    }
}