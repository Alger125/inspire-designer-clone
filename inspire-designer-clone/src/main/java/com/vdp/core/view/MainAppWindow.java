package com.vdp.core.view;

import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.Workflow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

/** Main workflow shell aligned with the manual's Workflow Window layout. */
public final class MainAppWindow extends JFrame {
    private static final long serialVersionUID = 1L;

    private final Workflow workflow = new Workflow("New Workflow 1");
    private final JLabel status = new JLabel("Workflow ready");
    private WorkflowCanvas workflowCanvas;

    public MainAppWindow() {
        InspireTheme.install();
        setTitle("Inspire Designer Clone - New Workflow 1.wfd");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar());

        JPanel root = new JPanel(new BorderLayout());
        root.add(createToolbar(), BorderLayout.NORTH);
        root.add(createWorkspace(), BorderLayout.CENTER);
        root.add(createBottomArea(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(InspireTheme.TOP_BAR);
        bar.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        for (String title : new String[]{"File", "Edit", "Workflow", "Window", "Help"}) {
            JMenu menu = new JMenu(title);
            menu.setForeground(new Color(225, 225, 225));
            bar.add(menu);
        }
        bar.add(javax.swing.Box.createHorizontalGlue());
        JLabel product = new JLabel("Inspire Designer");
        product.setForeground(new Color(190, 190, 190));
        product.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
        bar.add(product);
        return bar;
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(true);
        toolbar.setBackground(InspireTheme.TOOLBAR);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, InspireTheme.BORDER));
        toolbar.add(InspireTheme.toolbarButton("□", "New Workflow"));
        toolbar.add(InspireTheme.toolbarButton("↗", "Open Workflow"));
        toolbar.add(InspireTheme.toolbarButton("▣", "Save Workflow"));
        toolbar.addSeparator();
        toolbar.add(InspireTheme.toolbarButton("✂", "Cut"));
        toolbar.add(InspireTheme.toolbarButton("▤", "Copy"));
        toolbar.add(InspireTheme.toolbarButton("▥", "Paste"));
        toolbar.addSeparator();
        toolbar.add(InspireTheme.toolbarButton("✓", "Validate Workflow"));
        toolbar.add(InspireTheme.toolbarButton("▶", "Proof"));
        toolbar.addSeparator();
        toolbar.add(InspireTheme.toolbarButton("−", "Zoom Out"));
        toolbar.add(InspireTheme.toolbarButton("+", "Zoom In"));
        return toolbar;
    }

    private JSplitPane createWorkspace() {
        ModulePalette palette = new ModulePalette();
        JPanel templates = createTemplatesPanel();
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, palette, templates);
        left.setDividerLocation(475);
        left.setResizeWeight(0.78);
        left.setBorder(BorderFactory.createEmptyBorder());
        left.setPreferredSize(new Dimension(255, 650));

        workflowCanvas = new WorkflowCanvas(workflow, this::editModule, status::setText);
        JScrollPane canvasScroll = new JScrollPane(workflowCanvas);
        canvasScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, InspireTheme.BORDER));

        JPanel validation = createValidationPanel();
        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, canvasScroll, validation);
        center.setResizeWeight(0.82);
        center.setDividerLocation(535);
        center.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, center);
        workspace.setDividerLocation(255);
        workspace.setBorder(BorderFactory.createEmptyBorder());
        return workspace;
    }

    private JPanel createTemplatesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(InspireTheme.PANEL);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, InspireTheme.BORDER));
        JLabel title = new JLabel("Templates");
        title.setForeground(InspireTheme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 4));
        panel.add(title, BorderLayout.NORTH);

        JPanel entries = new JPanel();
        entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
        entries.setBackground(InspireTheme.PANEL);
        for (String name : new String[]{"□  Template 1", "□  Template 2", "□  Template 3"}) {
            JLabel item = new JLabel(name);
            item.setForeground(InspireTheme.TEXT);
            item.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 4));
            entries.add(item);
        }
        panel.add(entries, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createValidationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, InspireTheme.BORDER));
        JLabel title = new JLabel("Validation Results");
        title.setForeground(InspireTheme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 4));
        panel.add(title, BorderLayout.NORTH);
        JLabel empty = new JLabel("No validation has been run.");
        empty.setForeground(new Color(155, 155, 155));
        empty.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 10));
        panel.add(empty, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBottomArea() {
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setPreferredSize(new Dimension(10, 48));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setBackground(InspireTheme.TOOLBAR);
        String[] labels = {"ICM", "ICM Explorer", "Proof", "Sheet", "Data", "New Workflow 1", "Workflow"};
        for (String label : labels) {
            JButton tab = new JButton(label);
            tab.setFont(tab.getFont().deriveFont(Font.PLAIN, 11f));
            tab.setFocusable(false);
            tab.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 0, 0, InspireTheme.BORDER),
                    BorderFactory.createEmptyBorder(2, 10, 2, 10)));
            tab.setBackground(label.equals("Workflow") ? Color.WHITE : InspireTheme.TOOLBAR);
            tabs.add(tab);
        }

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(248, 248, 248));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, InspireTheme.BORDER));
        status.setForeground(InspireTheme.TEXT);
        status.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));
        statusBar.add(status, BorderLayout.WEST);
        JLabel login = new JLabel("Not logged");
        login.setForeground(InspireTheme.TEXT);
        login.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 10));
        statusBar.add(login, BorderLayout.EAST);

        bottom.add(tabs);
        bottom.add(statusBar);
        return bottom;
    }

    private void editModule(WorkflowNode node) {
        if (node.getModule() instanceof DataGeneratorModule generator) {
            DataGeneratorConfigDialog dialog = new DataGeneratorConfigDialog(this, generator);
            dialog.setVisible(true);
            if (dialog.isAccepted()) {
                node.repaint();
                status.setText("Data Generator configuration updated");
            }
        } else {
            status.setText("Configuration dialog not implemented for " + node.getModule().getName());
        }
    }
}
