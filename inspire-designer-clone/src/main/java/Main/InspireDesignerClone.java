package Main;

import com.vdp.core.view.MainAppWindow;
import javax.swing.SwingUtilities;

public class InspireDesignerClone {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainAppWindow window = new MainAppWindow();
            window.setExtendedState(MainAppWindow.MAXIMIZED_BOTH);
            window.setVisible(true);
        });
    }
}
