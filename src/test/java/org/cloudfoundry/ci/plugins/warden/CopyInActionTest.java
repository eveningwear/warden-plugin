package org.cloudfoundry.ci.plugins.warden;

import static org.junit.Assert.assertTrue;
import hudson.EnvVars;

import org.junit.Before;
import org.junit.Test;

public class CopyInActionTest {

    private CopyInAction copyInAction; // SUT

    private String srcPath;

    private String destPath;

    @Before
    public void setup() throws Exception {
    }

    @Test
    public void parameterParsing_1_Param() {
	copyInAction = new CopyInAction("/path1", "/path2");

	assertTrue(copyInAction.getSrcPath().equals("/path1"));
	assertTrue(copyInAction.getDestPath().equals("/path2"));
    }

    @Test
    public void parameterParsing_2_Params() {
	copyInAction = new CopyInAction("/parent/path1", "/parent/path2");

	assertTrue(copyInAction.getSrcPath().equals("/parent/path1"));
	assertTrue(copyInAction.getDestPath().equals("/parent/path2"));
    }
}
