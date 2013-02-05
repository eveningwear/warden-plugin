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
public class ScriptWrapper extends Builder {

    protected List<ScriptAction> stacks;

    @DataBoundConstructor
    public ScriptWrapper(List<ScriptAction> stacks) {
	this.stacks = stacks;
    }

    public List<ScriptAction> getStacks() {
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
	    Container.run(build, launcher, listener, listener.getLogger(),
		    envVars, stacks);
	} catch (TimeoutException e) {
	    listener.getLogger().append(
		    "ERROR run warden script operation timeout.");
	    build.setResult(Result.FAILURE);
	    success = false;
	}

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
	    return Messages.WardenScriptWrapper_DisplayName();
	}
    }
}
