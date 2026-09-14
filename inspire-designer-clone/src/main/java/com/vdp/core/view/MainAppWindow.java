package com.vdp.core.view;

import com.vdp.core.controller.WorkflowController;
import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.ProofRunResult;
import com.vdp.core.model.Workflow;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.HttpJsonDataInputModule;

/**
 * Main workflow shell aligned with the manual's Workflow and Proof windows.
 */
public final class MainAppWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String WORKFLOW_CARD = "WORKFLOW";
    private static final String DATA_PROOF_CARD = "DATA_PROOF";

    private final Workflow workflow = new Workflow("New Workflow 1");
    private final WorkflowController controller = new WorkflowController();
    private final ValidationResultsPanel validationPanel = new ValidationResultsPanel();
    private final DataProofPanel dataProofPanel = new DataProofPanel();
    private final JLabel status = new JLabel("Workflow ready");
    private final CardLayout workspaceLayout = new CardLayout();
    private final JPanel workspaceCards = new JPanel(workspaceLayout);
    private WorkflowCanvas workflowCanvas;
    private ProofRunResult lastProofResult;

    public MainAppWindow() {
        InspireTheme.install();
        setTitle("Inspire Designer Clone - New Workflow 1.wfd");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar());

        workspaceCards.add(createWorkflowWorkspace(), WORKFLOW_CARD);
        workspaceCards.add(dataProofPanel, DATA_PROOF_CARD);

        JPanel root = new JPanel(new BorderLayout());
        root.add(createToolbar(), BorderLayout.NORTH);
        root.add(workspaceCards, BorderLayout.CENTER);
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
        JButton validate = InspireTheme.toolbarButton("✓", "Validate Workflow");
        validate.addActionListener(event -> validateWorkflow());
        toolbar.add(validate);
        JButton runProof = InspireTheme.toolbarButton("▶", "Run Proof");
        runProof.addActionListener(event -> runProof());
        toolbar.add(runProof);
        toolbar.addSeparator();
        toolbar.add(InspireTheme.toolbarButton("−", "Zoom Out"));
        toolbar.add(InspireTheme.toolbarButton("+", "Zoom In"));
        return toolbar;
    }

    private JSplitPane createWorkflowWorkspace() {
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

        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, canvasScroll, validationPanel);
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

    private JPanel createBottomArea() {
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setPreferredSize(new Dimension(10, 48));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setBackground(InspireTheme.TOOLBAR);
        String[] labels = {"ICM", "ICM Explorer", "Proof", "Sheet", "Data", "New Workflow 1", "Workflow"};
        for (String label : labels) {
            JButton tab = createTab(label);
            if (label.equals("Workflow") || label.equals("New Workflow 1")) {
                tab.addActionListener(event -> showWorkflow());
            } else if (label.equals("Proof")) {
                tab.addActionListener(event -> showDataProof());
            }
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

    private JButton createTab(String label) {
        JButton tab = new JButton(label);
        tab.setFont(tab.getFont().deriveFont(Font.PLAIN, 11f));
        tab.setFocusable(false);
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 0, 0, InspireTheme.BORDER),
                BorderFactory.createEmptyBorder(2, 10, 2, 10)));
        tab.setBackground(label.equals("Workflow") ? Color.WHITE : InspireTheme.TOOLBAR);
        return tab;
    }

    private void validateWorkflow() {
        java.util.List<com.vdp.core.model.ValidationMessage> messages
                = controller.validateWorkflow(workflow);

        validationPanel.showMessages(messages);

        status.setText(
                messages.isEmpty()
                ? "Workflow validation passed"
                : "Workflow validation failed"
        );
    }

    private void runProof() {
        ProofRunResult result = controller.runProof(workflow, workflowCanvas.getSelectedModuleId());
        lastProofResult = result;
        validationPanel.showMessages(result.getValidationMessages());
        if (result.isSuccessful()) {
            dataProofPanel.showResult(result);
            workspaceLayout.show(workspaceCards, DATA_PROOF_CARD);
            status.setText("Proof completed: " + result.getSnapshots().size() + " module(s)");
        } else {
            workspaceLayout.show(workspaceCards, WORKFLOW_CARD);
            status.setText("Proof failed. Review Validation Results.");
        }
    }

    private void showWorkflow() {
        workspaceLayout.show(workspaceCards, WORKFLOW_CARD);
        status.setText("Workflow ready");
    }

    private void showDataProof() {
        boolean hasProof = lastProofResult != null && lastProofResult.isSuccessful();
        if (hasProof) {
            dataProofPanel.showLastOrEmpty();
        }
        workspaceLayout.show(workspaceCards, DATA_PROOF_CARD);
        status.setText(hasProof
                ? "Showing last Data Proof result" : "Run Proof to inspect data");
    }

    private void editModule(WorkflowNode node) {
    if (node.getModule() instanceof DataGeneratorModule generator) {
        DataGeneratorConfigDialog dialog =
                new DataGeneratorConfigDialog(this, generator);

        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            node.repaint();
            status.setText("Data Generator configuration updated");
        }

    } else if (node.getModule() instanceof DataInputModule input) {
        DataInputConfigDialog dialog =
                new DataInputConfigDialog(this, input);

        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            node.repaint();
            status.setText("Data Input configuration updated");
        }

    } else if (node.getModule() instanceof HttpJsonDataInputModule httpJson) {
        HttpJsonDataInputConfigDialog dialog =
                new HttpJsonDataInputConfigDialog(this, httpJson);

        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            node.repaint();
            status.setText("HTTP JSON Input configuration updated");
        }

    } else if (node.getModule() instanceof DataFilterModule filter) {
        DataFilterConfigDialog dialog =
                new DataFilterConfigDialog(this, filter);

        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            node.repaint();
            status.setText("Data Filter configuration updated");
        }

    } else {
        status.setText(
                "Configuration dialog not implemented for "
                + node.getModule().getName()
        );
    }
}
}
