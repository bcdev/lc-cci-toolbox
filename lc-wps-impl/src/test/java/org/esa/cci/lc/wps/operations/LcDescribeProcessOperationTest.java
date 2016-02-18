package org.esa.cci.lc.wps.operations;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.bc.wps.api.schema.ProcessDescriptionType;
import org.junit.*;

import java.util.List;

/**
 * @author hans
 */
public class LcDescribeProcessOperationTest {

    private LcDescribeProcessOperation describeProcessOperation;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void canGetSingleProcess() throws Exception {

        describeProcessOperation = new LcDescribeProcessOperation();
        List<ProcessDescriptionType> processes = describeProcessOperation.getProcesses("process1");

        assertThat(processes.size(), equalTo(1));
        assertThat(processes.get(0).getIdentifier().getValue(), equalTo("subsetting"));
        assertThat(processes.get(0).getTitle().getValue(), equalTo("Subsetting service"));
        assertThat(processes.get(0).getAbstract().getValue(), equalTo("This is a Subsetting Tool for LC-CCI"));
    }
}