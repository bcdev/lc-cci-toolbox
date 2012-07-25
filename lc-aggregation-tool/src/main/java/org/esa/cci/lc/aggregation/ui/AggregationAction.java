package org.esa.cci.lc.aggregation.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.cci.lc.aggregation.AggregationOp;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * @author Marco Peters
 */
public class AggregationAction  extends AbstractVisatAction{
    private ModelessDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (dialog == null) {
            dialog = createDialog(false, event.getCommand().getHelpId(), getAppContext());
        }
        dialog.show();
    }

    private static ModelessDialog createDialog(final boolean exitOnClose, final String helpId,
                                               final AppContext appContext) {
        return new SingleTargetProductDialog(appContext, "Landcover CCI Aggregation Tool",
                                               helpId) {
            @Override
            protected Product createTargetProduct() throws Exception {
                return null;
            }
            @Override
            protected void onClose() {
                super.onClose();
                if (exitOnClose) {
                    System.exit(0);
                }
            }
        };
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final DefaultAppContext context = new DefaultAppContext("dev0");
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(new AggregationOp.Spi());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ModelessDialog dialog = createDialog(true, "no_help_id", context);
                JDialog jDialog = dialog.getJDialog();
                jDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.show();
            }
        });
    }


}
