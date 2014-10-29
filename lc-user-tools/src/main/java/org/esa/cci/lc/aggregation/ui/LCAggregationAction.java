package org.esa.cci.lc.aggregation.ui;

import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * @author Marco Peters
 */
public class LCAggregationAction extends AbstractVisatAction {

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
        return new LCAggregationDialog(
                "LCCCI.Aggregate.Map", appContext, "Landcover CCI Aggregation Tool", helpId) {

            @Override
            protected void onClose() {
                super.onClose();
                if (exitOnClose) {
                    System.exit(0);
                }
            }
        };
    }

}
