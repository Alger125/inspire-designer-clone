package com.vdp.core.view;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class MainAppWindow extends JFrame {

    public MainAppWindow() {
        setTitle("Inspire Designer 14.0 Clone - Workflow Environment");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Barra de Menús
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu("File"));
        menuBar.add(new JMenu("Edit"));
        menuBar.add(new JMenu("Workflow"));
        menuBar.add(new JMenu("Window"));
        menuBar.add(new JMenu("Help"));
        setJMenuBar(menuBar);

        // Árbol de Módulos (Module Tree)
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Modules");
        
        DefaultMutableTreeNode dataInputs = new DefaultMutableTreeNode("Data Inputs");
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

        root.add(dataInputs);
        root.add(dataProcessing);
        root.add(designInputs);
        root.add(impositioning);
        root.add(outputs);

        JTree moduleTree = new JTree(new DefaultTreeModel(root));
        JScrollPane treeScrollPane = new JScrollPane(moduleTree);
        treeScrollPane.setMinimumSize(new Dimension(250, 0));

        // Lienzo de Trabajo (Workflow Area)
        JDesktopPane workflowArea = new JDesktopPane();
        workflowArea.setBackground(new Color(240, 240, 240));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, workflowArea);
        splitPane.setDividerLocation(250);
        splitPane.setOneTouchExpandable(true);

        getContentPane().add(splitPane, BorderLayout.CENTER);
    }
}