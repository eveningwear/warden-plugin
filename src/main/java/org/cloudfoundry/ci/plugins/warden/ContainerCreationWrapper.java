package org.cloudfoundry.ci.plugins.warden;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * 
 * @author <a href="mailto:jli@vmware.com">Jacky Li</a>
 * 
 */
public class ContainerCreationWrapper extends BuildWrapper {

    private Integer graceTime;
    private boolean ignorePassword;
    private boolean propagateEnv;
    private boolean limitItems;
    private String variableStr;
    private List<MountPoint> stacks;
    private String limitDisk;
    private String limitMemory;
    private String limitBandwidth;

    private ArrayList<String> variables;

    @DataBoundConstructor
    public ContainerCreationWrapper(Integer graceTime, boolean ignorePassword,
	    boolean propagateEnv, boolean limitItems, /* String variableStr, */
	    List<MountPoint> stacks, String limitDisk, String limitMemory,
	    String limitBandwidth) {
	this.graceTime = graceTime;
	this.ignorePassword = ignorePassword;
	this.propagateEnv = propagateEnv;
	this.limitItems = limitItems;
	this.stacks = stacks;
	// this.variableStr = variableStr;
	this.variables = new ArrayList<String>();
	this.limitDisk = limitDisk;
	this.limitMemory = limitMemory;
	this.limitBandwidth = limitBandwidth;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
	    BuildListener listener) throws IOException, InterruptedException {

	EnvVars envVars = build.getEnvironment(listener);
	envVars.overrideAll(build.getBuildVariables());

	final Map<String, String> envAdditions = new HashMap<String, String>();

	if (envVars.containsKey("VCAP_RUBY19")
		&& envVars.containsKey("VCAP_GIT")) {
	    envVars.put("WARDEN_RUNNING_PATH",
		    "$VCAP_RUBY19/bin:$VCAP_GIT/bin:$PATH");
	    envAdditions.put("WARDEN_RUNNING_PATH",
		    "$VCAP_RUBY19/bin:$VCAP_GIT/bin:$PATH");
	}

	boolean success = true;

	// WardenContainer wc = new WardenContainer(listener.getLogger(),
	// graceTime, stacks, envVars);

	try {
	    String handle = Container.create(build, launcher, listener,
		    listener.getLogger(), envVars, graceTime, stacks);
	    // String handle = wc.create(build, launcher, listener);
	    envAdditions.put("WARDEN_HANDLE", handle);
	    envVars.put("WARDEN_HANDLE", handle);

	    if (ignorePassword) {
		Container.ignorePassword(build, launcher, listener,
			listener.getLogger(), envVars);
	    }

	    if (propagateEnv) {
		for (String variable : variableStr.split(",")) {
		    variables.add(variable.trim());
		}
		Container.propagateEnvironment(build, launcher, listener,
			listener.getLogger(), envVars, variables);
	    }

	    if (limitItems) {
		Container.limitDisk(build, launcher, listener,
			listener.getLogger(), envVars, limitDisk);
		Container.limitMemory(build, launcher, listener,
			listener.getLogger(), envVars, limitMemory);
		Container.limitBandwidth(build, launcher, listener,
			listener.getLogger(), envVars, limitBandwidth);
	    }
	} catch (TimeoutException e) {
	    listener.getLogger().append(
		    "ERROR creating warden container operation timeout.");
	    build.setResult(Result.FAILURE);
	    success = false;
	}

	// If any stack fails to create then destroy them all
	if (!success) {
	    doTearDown();
	    return null;
	}

	return new Environment() {
	    @Override
	    public boolean tearDown(AbstractBuild build, BuildListener listener)
		    throws IOException, InterruptedException {

		return doTearDown();
	    }

	    @Override
	    public void buildEnvVars(Map<String, String> env) {
		EnvVars envVars = new EnvVars(env);
		envVars.putAll(envAdditions);
		env.putAll(envVars);
	    }
	};
    }

    protected boolean doTearDown() throws IOException, InterruptedException {
	return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

	private List<String> diskVolumes;
	private List<String> memoryVolumes;
	private List<String> bandwidthVolumes;

	@Override
	public String getDisplayName() {
	    return "Create Warden Container";
	}

	@Override
	public boolean isApplicable(AbstractProject<?, ?> item) {
	    return true;
	}

	public FormValidation doCheckGraceTime(
		@AncestorInPath AbstractProject<?, ?> project,
		@QueryParameter String value) throws IOException {
	    if (0 == value.length()) {
		return FormValidation.error("Empty Grace Time");
	    } else {
		try {
		    Integer.parseInt(value);
		} catch (Exception e) {
		    return FormValidation.error("Not Int Type");
		}
	    }
	    return FormValidation.ok();
	}

	public FormValidation doCheckStacks(
		@AncestorInPath AbstractProject<?, ?> project,
		@QueryParameter List<MountPoint> stacks) throws IOException {
	    if (stacks != null) {
		for (MountPoint stack : stacks) {
		    if (0 == stack.getSrcPath().length()) {
			return FormValidation.error("Empty Source Path");
		    } else if (0 == stack.getDestPath().length()) {
			return FormValidation.error("Empty Destination Path");
		    }
		}
	    }
	    return FormValidation.ok();
	}

	public List<String> getDiskVolumes() {
	    if (diskVolumes == null) {
		diskVolumes = new ArrayList<String>();
		diskVolumes.add("No Limit");
		diskVolumes.add("256M");
		diskVolumes.add("512M");
		diskVolumes.add("1G");
		diskVolumes.add("2G");
		diskVolumes.add("4G");
		diskVolumes.add("8G");
	    }

	    return diskVolumes;
	}

	public List<String> getMemoryVolumes() {
	    if (memoryVolumes == null) {
		memoryVolumes = new ArrayList<String>();
		memoryVolumes.add("No Limit");
		memoryVolumes.add("64M");
		memoryVolumes.add("128M");
		memoryVolumes.add("256M");
		memoryVolumes.add("512M");
		memoryVolumes.add("1G");
		memoryVolumes.add("2G");
		memoryVolumes.add("4G");
		memoryVolumes.add("8G");
	    }

	    return memoryVolumes;
	}

	public List<String> getBandwidthVolumes() {
	    if (bandwidthVolumes == null) {
		bandwidthVolumes = new ArrayList<String>();
		bandwidthVolumes.add("No Limit");
		bandwidthVolumes.add("64M");
		bandwidthVolumes.add("128M");
		bandwidthVolumes.add("256M");
		bandwidthVolumes.add("512M");
		bandwidthVolumes.add("1G");
		bandwidthVolumes.add("2G");
		bandwidthVolumes.add("4G");
		bandwidthVolumes.add("8G");
	    }

	    return bandwidthVolumes;
	}
    }

    public Integer getDefaultGraceTime() {
	return 200;
    }

    public Integer getGraceTime() {
	return graceTime;
    }

    public boolean getIgnorePassword() {
	return ignorePassword;
    }

    public boolean getLimitItems() {
	return limitItems;
    }

    public boolean isPropagateEnv() {
	return propagateEnv;
    }

    public String getVariableStr() {
	return variableStr;
    }

    public List<MountPoint> getStacks() {
	return stacks;
    }

    public String getLimitDisk() {
	return limitDisk;
    }

    public String getLimitMemory() {
	return limitMemory;
    }

    public String getLimitBandwidth() {
	return limitBandwidth;
    }

}
