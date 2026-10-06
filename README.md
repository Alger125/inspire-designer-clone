# Inspire Designer Clone

Un clon Open Source en Java (Swing) del sistema de flujos de trabajo **Inspire Designer**, enfocado en replicar la arquitectura de procesamiento de datos, los m\u00f3dulos de transformaci\u00f3n y el dise\u00f1o de la interfaz cl\u00e1sica (Workflow, Data Proof y Sheet Layout).

## 🚀 Estado del Proyecto (Milestone 1)

Actualmente, el proyecto replica la arquitectura principal del **Data Input** y los m\u00f3dulos de **Data Processing**, permitiendo crear flujos, conectarlos, validarlos y ejecutarlos en memoria.

### Entorno y Workflow (Cap\u00edtulo 3)
* **Lienzo (Canvas):** Drag & Drop de m\u00f3dulos, dibujado de conexiones curvas con detecci\u00f3n de colisiones y men\u00fa contextual.
* **Interfaz (MainAppWindow):** Men\u00fas superiores (`File`, `Edit`, `Workflow`, `Window`, `Help`) y barras de herramientas (`Standard`, `Zoom`, `Alignment`, `ICM`) id\u00e9nticos a la herramienta real.
* **Validation Results:** Verificaci\u00f3n de desconexiones o configuraciones incompletas antes de ejecutar.
* **Formatos de archivo:** Carga y guardado en archivos `.json` estructurados para retener configuraciones y posiciones en el lienzo.

### M\u00f3dulos de Datos
Se respeta la divisi\u00f3n en familias (*Data Inputs*, *Data Processing*) de la **Module Tree/Palette**.

1. **Data Input** (Soporta lectura de CSV, configuraci\u00f3n de encoding, l\u00edmites para *Proof* y rangos para *Production*).
2. **HTTP JSON Input** (Adici\u00f3n moderna: consume endpoints, procesa JSON paths y arrays din\u00e1micos).
3. **Data Generator** (Generaci\u00f3n secuencial en memoria).
4. **Data Filter** (Compuertas l\u00f3gicas, m\u00faltiples criterios, bifurcaci\u00f3n *Matched/Else*, fechas y rangos).
5. **Data Sorter** (M\u00faltiples criterios, ASC/DESC, eliminaci\u00f3n de duplicados, comparaci\u00f3n binaria/localizada, bifurcaci\u00f3n de nueva copia).

*Nota: Los m\u00f3dulos consultan la **Data Structure** din\u00e1micamente para configurar sus propiedades en la ventana (JTree), separando la vista de "Esquema" de las "Opciones".*

### Proofing y Layout
* **Data Proof Panel:** Visor estructurado tabular/registro por registro para debuggear el resultado intermedio de cualquier m\u00f3dulo en la cadena.
* **Sheet Layout Panel:** Base para el editor de plantillas. Incluye herramientas de inserci\u00f3n de texto, selecci\u00f3n t\u00e1ctil y arrastre.

## 🛠\ufe0f Tecnolog\u00edas y Ejecuci\u00f3n

* **Lenguaje:** Java 25
* **GUI:** Swing / AWT
* **Estructura:** Maven (`pom.xml`)
* **Dependencias:** `jackson-databind` (serializaci\u00f3n), `junit-jupiter` (testing)

### C\u00f3mo ejecutar
1. Clona el repositorio.
2. Compila el proyecto con Maven: `mvn clean install`
3. Inicia la aplicaci\u00f3n usando la clase principal: `com.vdp.core.Main.InspireDesignerClone`

## 📋 Pr\u00f3ximos Pasos (Roadmap)
* [ ] Nuevos m\u00f3dulos: *Data Transformer*, *Data Group By*.
* [ ] Proof Environment: Implementar los perfiles de salida y *Print from Proof*.
* [ ] Production Environment: Consola real de ejecuci\u00f3n desatendida.
* [ ] Expandir el Sheet Editor con figuras, im\u00e1genes y variables vinculadas al esquema.