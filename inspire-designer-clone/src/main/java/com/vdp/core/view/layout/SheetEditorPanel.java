package com.vdp.core.view.layout;

import com.vdp.core.model.layout.LayoutElement;
import com.vdp.core.model.layout.TextElement;
import com.vdp.core.view.InspireTheme;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class SheetEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private enum Tool { SELECT, TEXT, RECTANGLE, IMAGE }
    private Tool currentTool = Tool.SELECT;
    
    private final PageCanvas pageCanvas;
    private final List<LayoutElement> elements = new ArrayList<>();
    
    private LayoutElement selectedElement = null;
    private Point dragStart = null;
    private Rectangle selectionRect = null;

    private final JTextArea inlineEditor = new JTextArea();
    private TextElement editingElement = null;

    // UI Panels for Inspire Look & Feel
    private final JTree structureTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode treeRoot;
    
    private final JPanel propertiesPanel;
    private final JLabel propName = new JLabel("-");
    private final JLabel propX = new JLabel("-");
    private final JLabel propY = new JLabel("-");
    private final JLabel propW = new JLabel("-");
    private final JLabel propH = new JLabel("-");

    public SheetEditorPanel() {
        super(new BorderLayout());
        setBackground(InspireTheme.CANVAS);

        pageCanvas = new PageCanvas();
        pageCanvas.setLayout(null);
        
        inlineEditor.setVisible(false);
        inlineEditor.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215)));
        inlineEditor.setLineWrap(true);
        inlineEditor.setWrapStyleWord(true);
        inlineEditor.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitInlineEdit(); }
        });
        pageCanvas.add(inlineEditor);
        
        JScrollPane scroll = new JScrollPane(pageCanvas);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(new Color(230, 230, 230));

        // 1. TOP FORMATTING TOOLBAR
        JToolBar formatToolbar = new JToolBar();
        formatToolbar.setFloatable(false);
        formatToolbar.setBackground(InspireTheme.TOOLBAR);
        formatToolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, InspireTheme.BORDER));
        formatToolbar.add(new JLabel(" Font: "));
        JComboBox<String> fontCombo = new JComboBox<>(new String[]{"Arial", "Helvetica", "Times New Roman", "Courier"});
        fontCombo.setMaximumSize(new Dimension(150, 25));
        formatToolbar.add(fontCombo);
        formatToolbar.addSeparator();
        formatToolbar.add(new JLabel(" Size: "));
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(12, 6, 72, 1));
        sizeSpinner.setMaximumSize(new Dimension(60, 25));
        formatToolbar.add(sizeSpinner);
        formatToolbar.addSeparator();
        formatToolbar.add(InspireTheme.toolbarButton("B", "Bold"));
        formatToolbar.add(InspireTheme.toolbarButton("I", "Italic"));
        formatToolbar.add(InspireTheme.toolbarButton("U", "Underline"));

        // 2. LEFT TOOLBAR & STRUCTURE TREE
        JPanel leftContainer = new JPanel(new BorderLayout());
        
        JToolBar tools = new JToolBar(JToolBar.VERTICAL);
        tools.setFloatable(false);
        tools.setBackground(InspireTheme.PANEL);
        tools.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, InspireTheme.BORDER));
        
        JButton btnSelect = InspireTheme.toolbarButton("⬈", "Selección");
        JButton btnText = InspireTheme.toolbarButton("T", "Objeto de Texto");
        btnSelect.addActionListener(e -> setTool(Tool.SELECT));
        btnText.addActionListener(e -> setTool(Tool.TEXT));
        
        tools.add(btnSelect);
        tools.add(btnText);
        tools.add(InspireTheme.toolbarButton("□", "Rectángulo"));
        tools.add(InspireTheme.toolbarButton("🖼", "Imagen"));
        tools.add(InspireTheme.toolbarButton("▦", "Tabla"));

        treeRoot = new DefaultMutableTreeNode("Página 1");
        treeModel = new DefaultTreeModel(treeRoot);
        structureTree = new JTree(treeModel);
        structureTree.setBackground(InspireTheme.PANEL);
        JScrollPane treeScroll = new JScrollPane(structureTree);
        treeScroll.setPreferredSize(new Dimension(150, 0));
        treeScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, InspireTheme.BORDER));
        
        leftContainer.add(tools, BorderLayout.WEST);
        leftContainer.add(treeScroll, BorderLayout.CENTER);

        // 3. RIGHT PROPERTIES PANEL
        propertiesPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        propertiesPanel.setBackground(InspireTheme.PANEL);
        propertiesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, InspireTheme.BORDER),
                new EmptyBorder(10, 10, 10, 10)
        ));
        propertiesPanel.setPreferredSize(new Dimension(200, 0));
        
        propertiesPanel.add(new JLabel("Type:")); propertiesPanel.add(propName);
        propertiesPanel.add(new JLabel("X:")); propertiesPanel.add(propX);
        propertiesPanel.add(new JLabel("Y:")); propertiesPanel.add(propY);
        propertiesPanel.add(new JLabel("Width:")); propertiesPanel.add(propW);
        propertiesPanel.add(new JLabel("Height:")); propertiesPanel.add(propH);
        
        JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.setBackground(InspireTheme.PANEL);
        JLabel propsTitle = new JLabel(" Properties");
        propsTitle.setFont(InspireTheme.UI_FONT.deriveFont(Font.BOLD));
        propsTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rightContainer.add(propsTitle, BorderLayout.NORTH);
        rightContainer.add(propertiesPanel, BorderLayout.CENTER);
        rightContainer.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, InspireTheme.BORDER));

        // BUILD MAIN LAYOUT
        add(formatToolbar, BorderLayout.NORTH);
        add(leftContainer, BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);
        add(rightContainer, BorderLayout.EAST);
        
        setupMouseListeners();
    }
    
    private void setTool(Tool tool) {
        this.currentTool = tool;
        commitInlineEdit();
        if (tool == Tool.TEXT) {
            pageCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            pageCanvas.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void commitInlineEdit() {
        if (editingElement != null && inlineEditor.isVisible()) {
            editingElement.setText(inlineEditor.getText());
            inlineEditor.setVisible(false);
            editingElement = null;
            pageCanvas.repaint();
        }
    }

    private void updatePropertiesPanel() {
        if (selectedElement == null) {
            propName.setText("-");
            propX.setText("-");
            propY.setText("-");
            propW.setText("-");
            propH.setText("-");
        } else {
            propName.setText(selectedElement instanceof TextElement ? "Text Object" : "Object");
            Rectangle b = selectedElement.getBounds();
            propX.setText(b.x + " px");
            propY.setText(b.y + " px");
            propW.setText(b.width + " px");
            propH.setText(b.height + " px");
        }
    }

    private void updateStructureTree() {
        treeRoot.removeAllChildren();
        for (int i = 0; i < elements.size(); i++) {
            LayoutElement el = elements.get(i);
            String name = (el instanceof TextElement) ? "Text " + (i+1) : "Element " + (i+1);
            treeRoot.add(new DefaultMutableTreeNode(name));
        }
        treeModel.reload();
        for (int i = 0; i < structureTree.getRowCount(); i++) {
            structureTree.expandRow(i);
        }
    }

    private void setupMouseListeners() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                commitInlineEdit();
                if (currentTool == Tool.TEXT) {
                    dragStart = e.getPoint();
                    selectionRect = new Rectangle(dragStart);
                } else if (currentTool == Tool.SELECT) {
                    boolean clickedOnElement = false;
                    for (int i = elements.size() - 1; i >= 0; i--) {
                        LayoutElement el = elements.get(i);
                        if (el.contains(e.getX(), e.getY())) {
                            selectElement(el);
                            dragStart = e.getPoint();
                            clickedOnElement = true;
                            break;
                        }
                    }
                    if (!clickedOnElement) {
                        selectElement(null);
                        dragStart = e.getPoint();
                        selectionRect = new Rectangle(dragStart);
                    }
                }
                pageCanvas.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart == null) return;
                
                if (currentTool == Tool.TEXT || (currentTool == Tool.SELECT && selectedElement == null)) {
                    int x = Math.min(dragStart.x, e.getX());
                    int y = Math.min(dragStart.y, e.getY());
                    int width = Math.abs(dragStart.x - e.getX());
                    int height = Math.abs(dragStart.y - e.getY());
                    selectionRect = new Rectangle(x, y, width, height);
                } else if (currentTool == Tool.SELECT && selectedElement != null) {
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;
                    Rectangle b = selectedElement.getBounds();
                    selectedElement.setLocation(b.x + dx, b.y + dy);
                    dragStart = e.getPoint();
                    updatePropertiesPanel();
                }
                pageCanvas.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool == Tool.TEXT && selectionRect != null) {
                    if (selectionRect.width > 20 && selectionRect.height > 20) {
                        TextElement textObj = new TextElement("Text", selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
                        elements.add(textObj);
                        selectElement(textObj);
                        updateStructureTree();
                        setTool(Tool.SELECT);
                    }
                }
                selectionRect = null;
                dragStart = null;
                pageCanvas.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedElement instanceof TextElement) {
                    editingElement = (TextElement) selectedElement;
                    Rectangle b = editingElement.getBounds();
                    inlineEditor.setBounds(b);
                    inlineEditor.setText(editingElement.getText());
                    inlineEditor.setFont(editingElement.getFont());
                    inlineEditor.setVisible(true);
                    inlineEditor.requestFocus();
                }
            }
        };

        pageCanvas.addMouseListener(ma);
        pageCanvas.addMouseMotionListener(ma);
    }
    
    private void selectElement(LayoutElement el) {
        for (LayoutElement e : elements) {
            e.setSelected(false);
        }
        selectedElement = el;
        if (el != null) {
            el.setSelected(true);
        }
        updatePropertiesPanel();
    }

    private class PageCanvas extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int PAGE_WIDTH = 595;
        private final int PAGE_HEIGHT = 842;
        
        public PageCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(PAGE_WIDTH + 100, PAGE_HEIGHT + 100));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int x = Math.max(30, (getWidth() - PAGE_WIDTH) / 2);
            int y = 30;
            
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRect(x + 4, y + 4, PAGE_WIDTH, PAGE_HEIGHT);
            
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, PAGE_WIDTH, PAGE_HEIGHT);
            
            g2.setColor(InspireTheme.BORDER);
            g2.drawRect(x, y, PAGE_WIDTH, PAGE_HEIGHT);
            
            g2.setColor(new Color(0, 150, 255, 50));
            g2.drawRect(x + 50, y + 50, PAGE_WIDTH - 100, PAGE_HEIGHT - 100);
            
            for (LayoutElement el : elements) {
                el.paint(g2);
            }
            
            if (selectionRect != null) {
                g2.setColor(new Color(0, 120, 215, 100));
                g2.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
                if (currentTool == Tool.SELECT) {
                    g2.setColor(new Color(0, 120, 215, 30));
                    g2.fillRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
                }
            }
            g2.dispose();
        }
    }
}
