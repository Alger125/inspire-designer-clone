with open('src/test/java/com/vdp/core/view/WorkflowCanvasCopyPasteTest.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('canvas = new WorkflowCanvas(msg -> {});', 'canvas = new WorkflowCanvas(node -> {}, msg -> {});')

with open('src/test/java/com/vdp/core/view/WorkflowCanvasCopyPasteTest.java', 'w', encoding='utf-8') as f:
    f.write(content)
