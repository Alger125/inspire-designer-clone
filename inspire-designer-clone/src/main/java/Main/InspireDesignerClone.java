package Main;

import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.ExecutionContext;

public class InspireDesignerClone {

    public static void main(String[] args) {
        System.out.println("--- Iniciando Prueba de Lectura CSV (Entrega E1) ---");

        // 1. Instanciar el modulo[cite: 1]
        DataInputModule lectorDatos = new DataInputModule();
        
        // 2. Configurarlo con las reglas exactas del manual[cite: 2]
        lectorDatos.setInputFilePath("src/main/java/Main/clientes.csv");
        lectorDatos.setSkipFirstLines(1); // Ignoramos la primera linea de basura ("Ignora esta linea")[cite: 2]
        
        // 3. Crear un contexto y ejecutar el modulo[cite: 1]
        ExecutionContext contexto = new ExecutionContext();
        lectorDatos.execute(contexto);
        
        System.out.println("\nPrueba finalizada.");
    }
}