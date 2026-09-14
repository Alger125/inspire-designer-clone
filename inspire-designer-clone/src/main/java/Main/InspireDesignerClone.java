package Main;

import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.DataGeneratorModule; // <-- Importamos el nuevo módulo
import com.vdp.core.model.ExecutionContext;
import com.vdp.core.view.MainAppWindow;

public class InspireDesignerClone {

    public static void main(String[] args) {
        System.out.println("--- Iniciando Cadena de Datos (Entrega E2) ---");

        // 1. Modulo de Lectura
        DataInputModule lector = new DataInputModule();
        lector.setInputFilePath("C:/Users/Jon Jimz/Documents/NetBeansProjects/inspire-designer-clone/inspire-designer-clone/src/main/java/Main/clientes.csv"); 
        lector.setSkipFirstLines(1);
        
        // 2. Modulo de Filtrado
        DataFilterModule filtro = new DataFilterModule();
        filtro.setFieldName("Ubicacion"); 
        filtro.setCondition(DataFilterModule.Condition.CONTAINS);
        filtro.setFilterValue("Puebla"); 

        // 3. Ejecucion de la cadena
        ExecutionContext contexto = new ExecutionContext();
        lector.execute(contexto); 
        filtro.execute(contexto); 
        
        System.out.println("\nRegistros finales en memoria: " + contexto.getRecords().size());


        // --- PRUEBA DEL DATA GENERATOR (Etapa 1 - Paso 1 y 2) ---
        System.out.println("\n--- Probando Data Generator ---");
        DataGeneratorModule generador = new DataGeneratorModule();
        
        // Cambiamos el rango para probar (5 a 10)
        generador.setProperty("From", 5);
        generador.setProperty("To", 10);
        
        ExecutionContext contextoGen = new ExecutionContext();
        generador.execute(contextoGen);
        
        System.out.println("Nombres de columnas detectados: " + java.util.Arrays.toString(contextoGen.getColumnNames()));
        if (!contextoGen.getRecords().isEmpty()) {
            System.out.println("Primer valor generado: " + java.util.Arrays.toString(contextoGen.getRecords().get(0)));
            System.out.println("Ultimo valor generado: " + java.util.Arrays.toString(contextoGen.getRecords().get(contextoGen.getRecords().size() - 1)));
        }
        // --------------------------------------------------------


        // 4. Lanza tu interfaz grafica con el Drag & Drop activado (Entrega E3)
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                MainAppWindow ventanaPrincipal = new MainAppWindow();
                ventanaPrincipal.setTitle("Inspire Designer 14.0 Clone - Workflow Environment");
                ventanaPrincipal.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
                ventanaPrincipal.setVisible(true);
            }
        });
    }
}