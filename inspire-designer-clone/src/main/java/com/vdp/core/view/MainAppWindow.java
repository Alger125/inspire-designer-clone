package com.vdp.core.view;

import com.vdp.core.controller.WorkflowController;
import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.DataSorterModule;
import com.vdp.core.model.HttpJsonDataInputModule;
import com.vdp.core.model.ProofRunResult;
import com.vdp.core.model.WorkflowSerializer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Main workflow shell aligned with the manual's Workflow and Proof windows.
 */
public final class MainAppWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String WORKFLOW_CARD  = "WORKFLOW";
    private static final String DATA_PROOF_CARD = "DATA_PROOF";
    private static final String SHEET_CARD = "SHEET";

    // ── Core components ───────────────────────────────────────────────────────

    private final WorkflowController  controller     = new WorkflowController();
    private final ValidationResultsPanel validationPanel = new ValidationResultsPanel();
    private final DataProofPanel      dataProofPanel  = new DataProofPanel();
    private final JLabel              status          = new JLabel("Workflow ready");
    private final CardLayout          workspaceLayout = new CardLayout();
    private final JPanel              workspaceCards  = new JPanel(workspaceLayout);
    private WorkflowCanvas            workflowCanvas;
    private ProofRunResult            lastProofResult;

    /** Path of the last saved / opened file; null means "not yet saved". */
    private Path currentFile;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MainAppWindow() {
        InspireTheme.install();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar());

        workspaceCards.add(createWorkflowWorkspace(), WORKFLOW_CARD);
        workspaceCards.add(dataProofPanel, DATA_PROOF_CARD);
        workspaceCards.add(new com.vdp.core.view.layout.SheetEditorPanel(), SHEET_CARD);

        JPanel root = new JPanel(new BorderLayout());
        root.add(createToolbar(),   BorderLayout.NORTH);
        root.add(workspaceCards,    BorderLayout.CENTER);
        root.add(createBottomArea(), BorderLayout.SOUTH);
        setContentPane(root);

        updateTitle();  // set initial window title
    }

    // ── Menu bar ──────────────────────────────────────────────────────────────

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(InspireTheme.TOP_BAR);
        bar.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));

        bar.add(buildFileMenu());
        for (String title : new String[]{"Edit", "Workflow", "Window", "Help"}) {
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

    private JMenu buildFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setForeground(new Color(225, 225, 225));

        JMenuItem newItem  = new JMenuItem("New Workflow");
        JMenuItem openItem = new JMenuItem("Open Workflow...");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAs   = new JMenuItem("Save As...");

        newItem.addActionListener(event  -> handleNew());
        openItem.addActionListener(event -> handleOpen());
        saveItem.addActionListener(event -> handleSave());
        saveAs.addActionListener(event   -> handleSaveAs());

        menu.add(newItem);
        menu.add(openItem);
        menu.addSeparator();
        menu.add(saveItem);
        menu.add(saveAs);
        return menu;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(true);
        toolbar.setBackground(InspireTheme.TOOLBAR);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, InspireTheme.BORDER));

        JButton btnNew  = InspireTheme.toolbarButton("□",  "New Workflow");
        JButton btnOpen = InspireTheme.toolbarButton("↗",  "Open Workflow");
        JButton btnSave = InspireTheme.toolbarButton("▣",  "Save Workflow");
        btnNew.addActionListener(event  -> handleNew());
        btnOpen.addActionListener(event -> handleOpen());
        btnSave.addActionListener(event -> handleSave());

        toolbar.add(btnNew);
        toolbar.add(btnOpen);
        toolbar.add(btnSave);
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

        JButton zoomOut = InspireTheme.toolbarButton("−", "Zoom Out");
        JButton zoomIn  = InspireTheme.toolbarButton("+", "Zoom In");
        zoomOut.addActionListener(event -> workflowCanvas.zoomOut());
        zoomIn.addActionListener(event  -> workflowCanvas.zoomIn());
        toolbar.add(zoomOut);
        toolbar.add(zoomIn);

        return toolbar;
    }

    // ── Workspace layout ──────────────────────────────────────────────────────

    private JSplitPane createWorkflowWorkspace() {
        ModulePalette palette   = new ModulePalette();
        JPanel        templates = createTemplatesPanel();

        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, palette, templates);
        left.setDividerLocation(475);
        left.setResizeWeight(0.78);
        left.setBorder(BorderFactory.createEmptyBorder());
        left.setPreferredSize(new Dimension(255, 650));

        workflowCanvas = new WorkflowCanvas(this::editModule, status::setText);
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

    // ── Bottom area ───────────────────────────────────────────────────────────

    private JPanel createBottomArea() {
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setPreferredSize(new Dimension(10, 48));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setBackground(InspireTheme.TOOLBAR);
        String[] labels = {"ICM", "ICM Explorer", "Proof", "Sheet", "Data",
                            "New Workflow 1", "Workflow"};
        for (String label : labels) {
            JButton tab = createTab(label);
            if (label.equals("Workflow") || label.equals("New Workflow 1")) {
                tab.addActionListener(event -> showWorkflow());
            } else if (label.equals("Proof")) {
                tab.addActionListener(event -> showDataProof());
            } else if (label.equals("Sheet")) {
                tab.addActionListener(event -> showSheet());
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

    // ── File operations ───────────────────────────────────────────────────────

    private void handleNew() {
        if (!workflowCanvas.getWorkflow().getModules().isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Create a new workflow? Unsaved changes will be lost.",
                    "New Workflow",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return;
        }
        workflowCanvas.newWorkflow("New Workflow 1");
        validationPanel.showMessages(java.util.List.of());
        lastProofResult = null;
        currentFile     = null;
        workspaceLayout.show(workspaceCards, WORKFLOW_CARD);
        updateTitle();
        status.setText("New workflow created");
    }

    private void handleOpen() {
        JFileChooser chooser = buildChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        try {
            WorkflowSerializer.LoadResult result = WorkflowSerializer.load(file);
            workflowCanvas.loadFrom(result);
            validationPanel.showMessages(java.util.List.of());
            lastProofResult = null;
            currentFile     = file;
            workspaceLayout.show(workspaceCards, WORKFLOW_CARD);
            updateTitle();
            status.setText("Workflow loaded from " + file.getFileName());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not open file:\n" + exception.getMessage(),
                    "Open Workflow",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSave() {
        if (currentFile == null) {
            handleSaveAs();
        } else {
            saveToFile(currentFile);
        }
    }

    private void handleSaveAs() {
        JFileChooser chooser = buildChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        // Append .json if the user did not type an extension
        if (!file.getFileName().toString().endsWith(".json")) {
            file = file.resolveSibling(file.getFileName() + ".json");
        }
        saveToFile(file);
    }

    /** Core save logic shared by Save and Save As. */
    private void saveToFile(Path file) {
        try {
            WorkflowSerializer.save(
                    workflowCanvas.getWorkflow(),
                    workflowCanvas.getModuleTypes(),
                    workflowCanvas.getNodePositions(),
                    file);
            currentFile = file;
            updateTitle();
            status.setText("Workflow saved to " + file.getFileName());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save file:\n" + exception.getMessage(),
                    "Save Workflow",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser buildChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Inspire Workflow JSON (*.json)", "json"));
        if (currentFile != null) {
            chooser.setSelectedFile(currentFile.toFile());
        }
        return chooser;
    }

    // ── Workflow actions ──────────────────────────────────────────────────────

    private void validateWorkflow() {
        java.util.List<com.vdp.core.model.ValidationMessage> messages =
                controller.validateWorkflow(workflowCanvas.getWorkflow());
        validationPanel.showMessages(messages);
        status.setText(messages.isEmpty()
                ? "Workflow validation passed"
                : "Workflow validation failed");
    }

    private void runProof() {
        ProofRunResult result = controller.runProof(
                workflowCanvas.getWorkflow(),
                workflowCanvas.getSelectedModuleId());
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
        if (hasProof) dataProofPanel.showLastOrEmpty();
        workspaceLayout.show(workspaceCards, DATA_PROOF_CARD);
        status.setText(hasProof
                ? "Showing last Data Proof result"
                : "Run Proof to inspect data");
    }

    private void showSheet() {
        workspaceLayout.show(workspaceCards, SHEET_CARD);
        status.setText("Layout Designer ready");
    }

    // ── Module config dispatch ────────────────────────────────────────────────

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

        } else if (node.getModule() instanceof DataSorterModule sorter) {
            DataSorterConfigDialog dialog =
                    new DataSorterConfigDialog(this, sorter);
            dialog.setVisible(true);
            if (dialog.isAccepted()) {
                node.repaint();
                status.setText("Data Sorter configuration updated");
            }

        } else {
            status.setText("Configuration dialog not implemented for "
                    + node.getModule().getName());
        }
    }

    // ── Title management ──────────────────────────────────────────────────────

    private void updateTitle() {
        String name = workflowCanvas != null
                ? workflowCanvas.getWorkflow().getName()
                : "New Workflow 1";
        String file = currentFile != null
                ? currentFile.getFileName().toString()
                : name + ".wfd";
        setTitle("Inspire Designer Clone — " + file);
    }
}
