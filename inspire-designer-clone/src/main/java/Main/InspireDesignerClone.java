package Main;

import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.Workflow;
import com.vdp.core.view.MainAppWindow; // Importamos tu nueva ventana
import java.util.List;

public class InspireDesignerClone {

    public static void main(String[] args) {
        System.out.println("--- Iniciando Motor VDP (Clon Inspire Designer) ---");

        // (Aquí mantienes el código del Workflow y DataInputModule que ya tenías)
        Workflow miProyecto = new Workflow("Proyecto Prueba 01");
        DataInputModule lectorDatos = new DataInputModule();
        // ... (resto de tu código de prueba)

        // ¡ESTO ES LO NUEVO! Lanzamos la Interfaz Gráfica
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Creamos la ventana
                MainAppWindow ventanaPrincipal = new MainAppWindow();
                // Le ponemos un título
                ventanaPrincipal.setTitle("Inspire Designer 14.0 Clone - Workflow Environment");
                // Hacemos que se abra en pantalla completa
                ventanaPrincipal.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
                // La hacemos visible
                ventanaPrincipal.setVisible(true);
            }
        });
    }
}