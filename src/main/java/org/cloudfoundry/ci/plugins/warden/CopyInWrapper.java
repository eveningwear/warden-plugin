package org.cloudfoundry.ci.plugins.warden;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 
 * @author <a href="mailto:jli@vmware.com">Jacky Li</a>
 * 
 */
public class CopyInWrapper extends Builder {

    protected List<CopyInAction> stacks;

    @DataBoundConstructor
    public CopyInWrapper(List<CopyInAction> stacks) {
	this.stacks = stacks;
    }

    public List<CopyInAction> getStacks() {
	return stacks;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener)
	    throws InterruptedException, IOException {

	EnvVars envVars = build.getEnvironment(listener);
	envVars.overrideAll(build.getBuildVariables());

	final Map<String, String> envAdditions = new HashMap<String, String>();

	boolean success = true;

	try {
	    Container.copyIn(build, launcher, listener, listener.getLogger(),
		    envVars, stacks);
	} catch (TimeoutException e) {
	    listener.getLogger().append(
		    "ERROR warden copy-in operation timeout.");
	    build.setResult(Result.FAILURE);
	    success = false;
	}

	// If any stack fails to create then destroy them all
	if (!success) {
	    return false;
	}

	return true;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

	@Override
	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
	    return true;
	}

	@Override
	public String getDisplayName() {
	    return Messages.WardenCopyInWrapper_DisplayName();
	}
    }
}
