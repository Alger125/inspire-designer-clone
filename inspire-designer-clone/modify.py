with open('src/main/java/com/vdp/core/view/WorkflowCanvas.java', 'r', encoding='utf-8') as f:
    content = f.read()

target = '    Map<String, String> getModuleTypes() { return Map.copyOf(moduleTypes); }'
replacement = '    Map<String, String> getModuleTypes() { return Map.copyOf(moduleTypes); }\n    void registerModuleType(String moduleId, String type) { moduleTypes.put(moduleId, type); }'

content = content.replace(target, replacement)
with open('src/main/java/com/vdp/core/view/WorkflowCanvas.java', 'w', encoding='utf-8') as f:
    f.write(content)

with open('src/test/java/com/vdp/core/view/WorkflowCanvasCopyPasteTest.java', 'r', encoding='utf-8') as f:
    test_content = f.read()
test_content = test_content.replace('canvas.getModuleTypes().put(module.getId(), type);', 'canvas.registerModuleType(module.getId(), type);')
with open('src/test/java/com/vdp/core/view/WorkflowCanvasCopyPasteTest.java', 'w', encoding='utf-8') as f:
    f.write(test_content)
