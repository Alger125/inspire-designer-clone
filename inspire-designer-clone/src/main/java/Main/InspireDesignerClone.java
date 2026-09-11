package Main;

import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.Workflow;
import java.util.List;

public class InspireDesignerClone {

    public static void main(String[] args) {
        System.out.println("--- Iniciando Motor VDP (Clon Inspire Designer) ---");

        // 1. El usuario crea un nuevo archivo de trabajo (Workflow)
        Workflow miProyecto = new Workflow("Proyecto Prueba 01");
        System.out.println("Proyecto creado. Modulos actuales: " + miProyecto.getModules().size());

        // 2. El usuario arrastra un Data Input Module al lienzo[cite: 1, 2]
        DataInputModule lectorDatos = new DataInputModule();
        
        // 3. El sistema valida el módulo tal como exige la documentación[cite: 1]
        System.out.println("\nValidando modulo '" + lectorDatos.getName() + "' (sin configurar)...");
        List<String> errores = lectorDatos.validate();
        for (String error : errores) {
            System.out.println(error); // Debería imprimir que falta la ruta del archivo[cite: 1]
        }

        // 4. El usuario configura la ruta del archivo en la pestaña "Input File"[cite: 2]
        System.out.println("\nEl usuario configura la ruta del archivo...");
        lectorDatos.setInputFilePath("C:/mis_datos/clientes.csv");
        
        // 5. El sistema vuelve a validar antes de procesar (Proof o Production)[cite: 2]
        errores = lectorDatos.validate();
        if (errores.isEmpty()) {
            System.out.println("Validacion exitosa. El modulo esta configurado correctamente.");
        }

        // 6. Conectamos el módulo al Workflow
        miProyecto.addModule(lectorDatos);
        System.out.println("\nModulos en el workflow actual: " + miProyecto.getModules().size());
        System.out.println("Puertos de salida disponibles: " + lectorDatos.getOutputPorts().get(0).getType());
    }
}