package org.cloudfoundry.ci.plugins.warden;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 
 * @author <a href="mailto:jli@vmware.com">Jacky Li</a>
 * 
 */
public class WardenWrapper extends Builder {

    private final String wardenScript;
    private final String graceTime;
    protected List<MountPoint> stacks;

    @DataBoundConstructor
    public WardenWrapper(String wardenScript, String graceTime,
	    List<MountPoint> stacks) {
	this.wardenScript = wardenScript;
	this.graceTime = graceTime;
	this.stacks = stacks;
    }

    public String getWardenScript() {
	return wardenScript;
    }

    public String getGraceTime() {
	return graceTime;
    }

    public List<MountPoint> getStacks() {
	return stacks;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

	@Override
	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
	    return true;
	}

	@Override
	public String getDisplayName() {
	    return "Mocked Warden task";
	}
    }
}
