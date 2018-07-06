package org.esa.cci.lc.aggregation.ui;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.ui.AppContext;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.lang.reflect.Field;

/**
 * @author Marco Peters
 */
public class LCAggregationDialog extends SingleTargetProductDialog {

    private final String operatorName;
    private final Class<? extends Operator> operatorClass;
    private final BindingContext bindingContext;
    private final OperatorParameterSupport parameterSupport;
    private final SourceProductSelector sourceProductSelector;
    private JPanel form;

    protected LCAggregationDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(appContext, title, ID_APPLY_CLOSE, helpID);
        this.operatorName = operatorName;
        operatorClass = getOperatorClass(operatorName);

        parameterSupport = new OperatorParameterSupport(operatorClass);
        final PropertySet propertyContainer = parameterSupport.getPopertySet();
        bindingContext = new BindingContext(propertyContainer);
        sourceProductSelector = createSourceProductSelector();
    }

    @Override
    public int show() {
        if (form == null) {
            initForm();
            if (getJDialog().getJMenuBar() == null) {
                final OperatorMenu operatorMenu = new OperatorMenu(getJDialog(), operatorClass, parameterSupport,
                                                                   getHelpID());
                getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
            }
        }
        setContent(form);
        return super.show();
    }

    @Override
    protected void onApply() {
        // disabling the implementation of super class

        try {
            Product targetProduct = createTargetProduct();
            if (targetProduct == null) {
                throw new NullPointerException("Target product is null.");
            }
            getAppContext().getProductManager().addProduct(targetProduct);
        } catch (Throwable t) {
            handleInitialisationError(t);
        }
    }


    @Override
    protected Product createTargetProduct() throws Exception {
        final TargetProductCreator targetProductCreator = new TargetProductCreator(getAppContext());
        targetProductCreator.executeWithBlocking();
        return targetProductCreator.get();
    }

    private void initForm() {
        final PropertyPane parametersPane = new PropertyPane(bindingContext);

        form = new JPanel(new BorderLayout(4, 4));
        form.setBorder(new EmptyBorder(4, 4, 4, 4));
        form.add(sourceProductSelector.createDefaultPanel(), BorderLayout.NORTH);
        form.add(parametersPane.createPanel(), BorderLayout.SOUTH);
    }

    private SourceProductSelector createSourceProductSelector() {
        final Field[] fields = operatorClass.getDeclaredFields();
        SourceProductSelector sourceProductSelector = null;
        for (Field field : fields) {
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (annot != null) {
                sourceProductSelector = new SourceProductSelector(getAppContext());
                break;
            }
        }
        if (sourceProductSelector == null) {
            throw new IllegalStateException("Could not create user interface. No source product annotation found.");
        }
        return sourceProductSelector;
    }

    private Class<? extends Operator> getOperatorClass(String operatorName) {
        GPF gpfInstance = GPF.getDefaultInstance();
        OperatorSpiRegistry operatorSpiRegistry = gpfInstance.getOperatorSpiRegistry();
        OperatorSpi operatorSpi = operatorSpiRegistry.getOperatorSpi(operatorName);
        return operatorSpi.getOperatorClass();
    }

    private class TargetProductCreator extends ProgressMonitorSwingWorker<Product, Void> {

        private AppContext appContext;

        protected TargetProductCreator(AppContext appContext) {
            super(LCAggregationDialog.this.getJDialog(), "Creating target product");
            this.appContext = appContext;
        }

        @Override
        protected Product doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Aggregating...", 100);

            final Product targetProduct = GPF.createProduct(operatorName, parameterSupport.getParameterMap(),
                                                            sourceProductSelector.getSelectedProduct());
            pm.worked(99);
            pm.done();

            return targetProduct;
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (Exception e) {
                appContext.handleError(String.format("An internal Error occurred:\n%s", e.getMessage()), e);
            }
        }

    }

}
