package com.vdp.core.view;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class MainAppWindow extends JFrame {

    public MainAppWindow() {
        // 1. Configuración base de la ventana
        setTitle("Inspire Designer 14.0 Clone - Workflow Environment");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 2. Barra de Menús exacta del manual
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu("File"));
        menuBar.add(new JMenu("Edit"));
        menuBar.add(new JMenu("Workflow"));
        menuBar.add(new JMenu("Window"));
        menuBar.add(new JMenu("Help"));
        setJMenuBar(menuBar);

        // 3. Crear el Árbol de Módulos (Module Tree)
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Modules");
        
        // Familias principales documentadas
        DefaultMutableTreeNode dataInputs = new DefaultMutableTreeNode("Data Inputs");
        // Módulos exactos de entrada[cite: 2]
        dataInputs.add(new DefaultMutableTreeNode("Data Input"));
        dataInputs.add(new DefaultMutableTreeNode("Internal Data Input"));
        dataInputs.add(new DefaultMutableTreeNode("JSON Data Input"));
        dataInputs.add(new DefaultMutableTreeNode("XML Data Input"));
        
        DefaultMutableTreeNode dataProcessing = new DefaultMutableTreeNode("Data Processing");
        dataProcessing.add(new DefaultMutableTreeNode("Data Filter"));
        dataProcessing.add(new DefaultMutableTreeNode("1-1 Merger"));
        
        DefaultMutableTreeNode designInputs = new DefaultMutableTreeNode("Design Inputs");
        designInputs.add(new DefaultMutableTreeNode("Layout"));

        DefaultMutableTreeNode impositioning = new DefaultMutableTreeNode("Impositioning");
        DefaultMutableTreeNode outputs = new DefaultMutableTreeNode("Outputs");

        // Agregar familias a la raíz
        root.add(dataInputs);
        root.add(dataProcessing);
        root.add(designInputs);
        root.add(impositioning);
        root.add(outputs);

        // Configurar el componente visual del árbol
        JTree moduleTree = new JTree(new DefaultTreeModel(root));
        JScrollPane treeScrollPane = new JScrollPane(moduleTree);
        treeScrollPane.setMinimumSize(new Dimension(250, 0)); // Ancho mínimo del panel izquierdo

        // 4. Crear el Lienzo de Trabajo (Workflow Area)[cite: 2]
        JDesktopPane workflowArea = new JDesktopPane();
        workflowArea.setBackground(new Color(240, 240, 240)); // Color de fondo claro

        // 5. Unir el árbol y el lienzo con un SplitPane[cite: 2]
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, workflowArea);
        splitPane.setDividerLocation(250); // Posición inicial del divisor
        splitPane.setOneTouchExpandable(true);

        // Agregar la división a la ventana principal
        getContentPane().add(splitPane, BorderLayout.CENTER);
    }
}