package com.vdp.core.view;

import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.InspireModule;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainAppWindow extends JFrame {

    // --- MAPA PARA CONECTAR VISTA CON MODELO ---
    private Map<JInternalFrame, InspireModule> nodeToModuleMap = new HashMap<>();

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
            g2.setStroke(new BasicStroke(3));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(150, 150, 150));
            for (Wire wire : wires) {
                Point p1 = getRightPortCoord(wire.source);
                Point p2 = getLeftPortCoord(wire.target);
                drawBezierCurve(g2, p1.x, p1.y, p2.x, p2.y);
            }

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
        setJMenuBar(menuBar);

        // --- POBLANDO EL ÁRBOL NUEVAMENTE ---
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Modules");
        
        DefaultMutableTreeNode dataInputs = new DefaultMutableTreeNode("Data Inputs");
        dataInputs.add(new DefaultMutableTreeNode("Data Input"));
        dataInputs.add(new DefaultMutableTreeNode("Data Generator")); // Agregamos el nuevo
        
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

    private void crearModuloVisual(String nombreModulo, int x, int y) {
        if (nombreModulo.equals("Data Input") || nombreModulo.equals("Data Filter") || nombreModulo.equals("Data Generator")) {
            
            // 1. INSTANCIAR EL MÓDULO REAL DE JAVA
            InspireModule instanciaModelo = null;
            if (nombreModulo.equals("Data Generator")) {
                instanciaModelo = new DataGeneratorModule();
            } else if (nombreModulo.equals("Data Input")) {
                instanciaModelo = new DataInputModule();
            } else if (nombreModulo.equals("Data Filter")) {
                instanciaModelo = new DataFilterModule();
            }

            // 2. CREAR LA VENTANITA VISUAL
            JInternalFrame moduloUI = new JInternalFrame(nombreModulo, true, true, true, true);
            moduloUI.setSize(180, 100);
            moduloUI.setLocation(x, y);
            
            // 3. VINCULAR LA VISTA CON EL MODELO EN NUESTRO DICCIONARIO
            nodeToModuleMap.put(moduloUI, instanciaModelo);

            JPanel panelPrincipal = new JPanel(new BorderLayout());
            panelPrincipal.add(new JLabel("⚙️ " + nombreModulo, SwingConstants.CENTER), BorderLayout.CENTER);
            
            // EVENTO DE DOBLE CLIC PARA ABRIR CONFIGURACIÓN
            moduloUI.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        InspireModule moduloClickeado = nodeToModuleMap.get(moduloUI);
                        
                        // Si es el Data Generator, abrimos su ventana especial
                        if (moduloClickeado instanceof DataGeneratorModule) {
                            // CORRECCIÓN: Hacemos el cast aquí para poder acceder a getProperty
                            DataGeneratorModule generador = (DataGeneratorModule) moduloClickeado;
                            DataGeneratorConfigDialog dialog = new DataGeneratorConfigDialog(MainAppWindow.this, generador);
                            dialog.setVisible(true);
                            if (dialog.isOk()) {
                                System.out.println("Modulo actualizado. Nuevo rango: " + generador.getProperty("From") + " al " + generador.getProperty("To"));
                            }
                        }
                    }
                }
            });

            // Puertos visuales (solo el Data Filter tiene entrada actualmente en nuestra lógica)
            if (!nombreModulo.equals("Data Generator") && !nombreModulo.equals("Data Input")) {
                JPanel puertoEntrada = new JPanel();
                puertoEntrada.setBackground(new Color(46, 204, 113)); 
                puertoEntrada.setPreferredSize(new Dimension(15, 0));
                panelPrincipal.add(puertoEntrada, BorderLayout.WEST);
            }

            JPanel puertoSalida = new JPanel();
            puertoSalida.setBackground(new Color(52, 152, 219)); 
            puertoSalida.setPreferredSize(new Dimension(15, 0));
            puertoSalida.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            puertoSalida.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { workflowArea.draggingSource = moduloUI; }
                public void mouseReleased(MouseEvent e) {
                    Point dropPoint = SwingUtilities.convertPoint(puertoSalida, e.getPoint(), workflowArea);
                    JInternalFrame target = null;
                    for (Component c : workflowArea.getComponents()) {
                        if (c instanceof JInternalFrame && c != moduloUI) {
                            if (c.getBounds().contains(dropPoint)) {
                                target = (JInternalFrame) c;
                                break;
                            }
                        }
                    }
                    if (target != null) {
                        workflowArea.wires.add(new Wire(moduloUI, target));
                        System.out.println("🔗 Conectados visualmente!");
                    }
                    workflowArea.draggingSource = null;
                    workflowArea.dragPoint = null;
                    workflowArea.repaint();
                }
            });

            puertoSalida.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    workflowArea.dragPoint = SwingUtilities.convertPoint(puertoSalida, e.getPoint(), workflowArea);
                    workflowArea.repaint();
                }
            });
            
            panelPrincipal.add(puertoSalida, BorderLayout.EAST);
            moduloUI.add(panelPrincipal);
            
            moduloUI.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentMoved(java.awt.event.ComponentEvent e) { workflowArea.repaint(); }
            });

            moduloUI.setVisible(true);
            workflowArea.add(moduloUI); 
            try { moduloUI.setSelected(true); } catch (Exception e) {}
        }
    }
}