package com.vdp.core.view;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class MainAppWindow extends JFrame {

    private JDesktopPane workflowArea; 

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
        dataInputs.add(new DefaultMutableTreeNode("Internal Data Input"));
        
        DefaultMutableTreeNode dataProcessing = new DefaultMutableTreeNode("Data Processing");
        dataProcessing.add(new DefaultMutableTreeNode("Data Filter"));
        dataProcessing.add(new DefaultMutableTreeNode("1-1 Merger"));
        
        DefaultMutableTreeNode outputs = new DefaultMutableTreeNode("Outputs");

        root.add(dataInputs);
        root.add(dataProcessing);
        root.add(outputs);

        JTree moduleTree = new JTree(new DefaultTreeModel(root));
        
        // --- 1. ACTIVAR DRAG EN EL ÁRBOL ---
        moduleTree.setDragEnabled(true);
        moduleTree.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY; 
            }
            @Override
            protected Transferable createTransferable(JComponent c) {
                JTree tree = (JTree) c;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node != null && node.isLeaf()) {
                    return new StringSelection(node.getUserObject().toString());
                }
                return null;
            }
        });

        JScrollPane treeScrollPane = new JScrollPane(moduleTree);
        treeScrollPane.setMinimumSize(new Dimension(250, 0));

        // --- 2. CONFIGURAR EL LIENZO (Workflow Area) ---
        workflowArea = new JDesktopPane();
        workflowArea.setBackground(new Color(240, 240, 240));

        // Activar el DROP (soltar) en el lienzo
        workflowArea.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }
            @Override
            public boolean importData(TransferSupport support) {
                try {
                    String nombreModulo = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    Point dropLocation = support.getDropLocation().getDropPoint();
                    crearModuloVisual(nombreModulo, dropLocation.x, dropLocation.y);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, workflowArea);
        splitPane.setDividerLocation(250);
        splitPane.setOneTouchExpandable(true);

        getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    // --- 3. LÓGICA PARA DIBUJAR LA CAJITA DEL MÓDULO ---
    private void crearModuloVisual(String nombreModulo, int x, int y) {
        if (nombreModulo.equals("Data Input") || nombreModulo.equals("Data Filter")) {
            JInternalFrame moduloUI = new JInternalFrame(nombreModulo, true, true, true, true);
            moduloUI.setSize(180, 100);
            moduloUI.setLocation(x, y);
            
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("⚙️ Configurar...", SwingConstants.CENTER), BorderLayout.CENTER);
            moduloUI.add(panel);
            
            moduloUI.setVisible(true);
            workflowArea.add(moduloUI); 
            
            try {
                moduloUI.setSelected(true); 
            } catch (java.beans.PropertyVetoException e) {}
        }
    }
}