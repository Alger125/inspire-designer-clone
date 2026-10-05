with open('src/main/java/com/vdp/core/view/WorkflowCanvas.java', 'r', encoding='utf-8') as f:
    content = f.read()

target1 = '''<<<<<<< HEAD
            case "Data Concatenator" -> new DataConcatenatorModule();
=======
            case "Data Transformer" -> new com.vdp.core.model.DataTransformerModule();
>>>>>>> abfb407 (feat: add Data Transformer module with mapping dialog, serialization and tests)'''
replacement1 = '''            case "Data Transformer" -> new com.vdp.core.model.DataTransformerModule();
            case "Data Concatenator" -> new DataConcatenatorModule();'''

target2 = '''<<<<<<< HEAD
        return type.equals("Data Filter") || type.equals("Data Sorter") || type.equals("Data Concatenator");
=======
        return type.equals("Data Filter") || type.equals("Data Sorter")
                || type.equals("Data Transformer");
>>>>>>> abfb407 (feat: add Data Transformer module with mapping dialog, serialization and tests)'''
replacement2 = '''        return type.equals("Data Filter") || type.equals("Data Sorter")
                || type.equals("Data Transformer") || type.equals("Data Concatenator");'''

content = content.replace(target1, replacement1).replace(target2, replacement2)
with open('src/main/java/com/vdp/core/view/WorkflowCanvas.java', 'w', encoding='utf-8') as f:
    f.write(content)
