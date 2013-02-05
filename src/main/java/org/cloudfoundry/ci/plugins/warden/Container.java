package org.cloudfoundry.ci.plugins.warden;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.tasks.Shell;
import hudson.Launcher;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import jenkins.model.Jenkins;

/**
 * Core class for interacting with Warden in the back-end system.
 * 
 * @author <a href="mailto:jli@vmware.com">Jacky Li</a>
 * 
 */
public class Container {

    private PrintStream logger;

    private List<ScriptAction> scripts;
    private EnvVars envVars;

    private String warden_repl_v2;
    private String warden_instance_location;

    private static Container instance;

    public Container(PrintStream logger) {
	this.logger = logger;
    }

    private static Container getInstance(PrintStream logger, EnvVars envVars) {
	if (instance == null) {
	    instance = new Container(logger);
	}

	instance.envVars = envVars;

	if (envVars.containsKey("WARDEN_REPL_V2")) {
	    instance.warden_repl_v2 = envVars.get("WARDEN_REPL_V2");
	} else {
	    instance.warden_repl_v2 = "/var/vcap/packages/warden/warden/bin/warden";
	}

	if (envVars.containsKey("WARDEN_INSTANCE")) {
	    instance.warden_instance_location = envVars.get("WARDEN_INSTANCE");
	} else {
	    instance.warden_instance_location = "/var/vcap/data/warden/depot";
	}

	return instance;
    }

    public static String create(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars, Integer graceTime,
	    List<MountPoint> mountPoints) throws IOException, TimeoutException,
	    InterruptedException {
	return Container.getInstance(logger, envVars).create(build, launcher,
		listener, graceTime, mountPoints);
    }

    public static void destory(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars) throws IOException,
	    TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).destory(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener);
    }

    public static void copyIn(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars,
	    List<CopyInAction> copyInActions) throws IOException,
	    TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).copyIn(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener,
		copyInActions);
    }

    public static void copyOut(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars,
	    List<CopyOutAction> copyOutActions) throws IOException,
	    TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).copyOut(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener,
		copyOutActions);
    }

    public static void run(AbstractBuild<?, ?> build, final Launcher launcher,
	    final BuildListener listener, PrintStream logger, EnvVars envVars,
	    List<ScriptAction> scripts) throws IOException, TimeoutException,
	    InterruptedException {
	Container.getInstance(logger, envVars).run(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener,
		scripts);
    }

    public static void ignorePassword(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars) throws IOException,
	    TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).ignorePassword(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener);
    }

    public static void propagateEnvironment(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars, ArrayList<String> variables)
	    throws IOException, TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).propagateEnvironment(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener,
		envVars, variables);
    }

    /*
     * The functionality of limit disk, memory and bandwidth is similar, but the
     * reason why to keep them separately is because the sub command are totally
     * different and it's hard to merge them into one function. So, for some
     * extensibility in the future, keep them separately.
     */
    public static void limitDisk(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars, String volume)
	    throws IOException, TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).limitDisk(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener,
		envVars, volume);
    }

    public static void limitMemory(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars, String volume)
	    throws IOException, TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).limitMemory(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener,
		envVars, volume);
    }

    public static void limitBandwidth(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    PrintStream logger, EnvVars envVars, String volume)
	    throws IOException, TimeoutException, InterruptedException {
	Container.getInstance(logger, envVars).limitBandwidth(
		envVars.get("WARDEN_HANDLE"), build, launcher, listener,
		envVars, volume);
    }

    private FilePath generateScript(AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener, String script)
	    throws InterruptedException {
	// First we create the script in a temporary directory.
	FilePath ws = build.getWorkspace(), scriptFile;

	try {
	    // Create a file in the system temporary directory with our script
	    // in it.
	    scriptFile = ws.createTextTempFile(build.getProject().getName(),
		    ".sh", script, false);

	    return scriptFile;
	} catch (IOException e) {
	    Util.displayIOException(e, listener);
	    e.printStackTrace(listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToProduceScript()));
	}

	return null;
    }

    private String getEnvironment() {
	StringBuilder sb = new StringBuilder();

	sb.append("export PATH=");
	if (envVars.containsKey("WARDEN_RUNNING_PATH")) {
	    sb.append(envVars.get("WARDEN_RUNNING_PATH"));
	} else {
	    sb.append("$PATH");
	}

	sb.append(";\n");

	return sb.toString();
    }

    private String wrapWardenRootCommand(String handle, String command) {
	StringBuilder sb = new StringBuilder();
	sb.append("sudo ssh -T -F ");
	sb.append(warden_instance_location);
	sb.append("/");
	sb.append(handle);
	sb.append("/ssh/ssh_config root@container ");
	sb.append("\"");
	sb.append(command);
	sb.append("\"\n");
	return sb.toString();
    }

    private String getIgnorePassword(String handle) {
	StringBuilder sb = new StringBuilder();

	sb.append(wrapWardenRootCommand(handle, "chmod 740 /etc/sudoers"));
	sb.append(wrapWardenRootCommand(handle,
		"echo \\\"vcap  ALL=(ALL) NOPASSWD: ALL\\\" >> /etc/sudoers"));
	sb.append(wrapWardenRootCommand(handle, "chmod 440 /etc/sudoers"));

	// Add a workaround to bypass a stopper due to missing insserv in the
	// new stemcell
	sb.append(wrapWardenRootCommand(handle,
		"[ -f \"/sbin/insserv\" ] || ln -s /usr/lib/insserv/insserv /sbin/insserv"));
	return sb.toString();
    }

    private String wrapEnvironmentCommand(String key, String value) {
	StringBuilder sb = new StringBuilder();
	sb.append("export ");
	sb.append(key);
	sb.append("=");
	sb.append(value);
	sb.append("\n");
	return sb.toString();
    }

    private String getPropagateEnvironment(String handle, EnvVars envVars,
	    ArrayList<String> variables) {
	StringBuilder sb = new StringBuilder();

	for (String key : variables) {
	    String value = envVars.containsKey(key) ? envVars.get(key) : "";
	    sb.append(wrapWardenRootCommand(handle, "echo \\\"" + key + "="
		    + value + "\\\" >> /etc/environment"));
	}
	return sb.toString();
    }

    private String[] buildCommandLine(FilePath scriptFile) {
	// Respect shebangs
	String script = "";
	if (script.startsWith("#!")) {
	    // Find first line, or just entire script if it's one line.
	    int end = script.indexOf('\n');
	    if (end < 0)
		end = script.length();

	    String shell = script.substring(0, end).trim();
	    shell = shell.substring(2);

	    List<String> args = new ArrayList<String>(Arrays.asList(Util
		    .tokenize(shell)));
	    args.add(scriptFile.getRemote());

	    return args.toArray(new String[args.size()]);
	} else {
	    Shell.DescriptorImpl shellDescriptor = Jenkins.getInstance()
		    .getDescriptorByType(Shell.DescriptorImpl.class);
	    String shell = shellDescriptor.getShellOrDefault(scriptFile
		    .getChannel());
	    return new String[] { shell, "-xe", scriptFile.getRemote() };
	}
    }

    private Integer convertStringToInt(String volume) {
	Integer volumeInByte = -1;
	if (volume.endsWith("G")) {
	    volumeInByte = Integer.parseInt(volume.split("G")[0]) * 1024 * 1024;
	} else if (volume.endsWith("M")) {
	    volumeInByte = Integer.parseInt(volume.split("M")[0]) * 1024;
	}

	return volumeInByte;
    }

    private String create(AbstractBuild<?, ?> build, final Launcher launcher,
	    final BuildListener listener, Integer graceTime,
	    List<MountPoint> mountPoints) throws IOException, TimeoutException,
	    InterruptedException {

	logger.println("Creating Warden Container");

	FilePath ws = build.getWorkspace(), scriptFile;

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- create");

	Integer count = 0;
	for (MountPoint mp : mountPoints) {
	    if (mp.isWell()) {
		sb.append(" --bind_mounts[" + count + "].src_path ");
		sb.append(mp.getSrcPath());
		sb.append(" --bind_mounts[" + count + "].dst_path ");
		sb.append(mp.getDestPath());
		sb.append(" --bind_mounts[" + count + "].mode ");
		sb.append(mp.getMntMode());

		count++;
	    }
	}

	sb.append(" --grace_time ");
	sb.append(graceTime);
	sb.append(" | awk '{print $3}'");

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();

	//if(true) return "12345";

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(commandOutput).pwd(ws)
		.join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return null;
	}

	String handle = commandOutput.toString().trim();
	this.logger.println("Warden handler: " + handle);
	return handle;
    }

    /**
     * @return
     */
    private boolean destory(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener)
	    throws IOException, TimeoutException, InterruptedException {
	logger.println("Destroying Warden Container");

	FilePath ws = build.getWorkspace(), scriptFile;

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- destory ");
	sb.append(" --handle ");
	sb.append(handle);

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private void copyIn(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    List<CopyInAction> copyInActions) throws IOException,
	    TimeoutException, InterruptedException {

	logger.println("Copy files into Warden Container");

	for (CopyInAction cia : copyInActions) {
	    if (cia.isWell()) {
		copyIn(handle, build, launcher, listener, cia.getSrcPath(),
			cia.getDestPath());
	    }
	}
    }

    private boolean copyIn(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    String srcPath, String dstPath) throws IOException,
	    TimeoutException, InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- copy_in ");
	sb.append(" --handle ");
	sb.append(handle);
	sb.append(" --src_path ");
	sb.append(srcPath);
	sb.append(" --dst_path ");
	sb.append(dstPath);

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private void copyOut(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    List<CopyOutAction> copyOutActions) throws IOException,
	    TimeoutException, InterruptedException {

	logger.println("Copy files from Warden Container");

	for (CopyOutAction coa : copyOutActions) {
	    if (coa.isWell()) {
		copyOut(handle, build, launcher, listener, coa.getSrcPath(),
			coa.getDestPath());
	    }
	}
    }

    private boolean copyOut(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    String srcPath, String dstPath) throws IOException,
	    TimeoutException, InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- copy_out ");
	sb.append(" --handle ");
	sb.append(handle);
	sb.append(" --src_path ");
	sb.append(srcPath);
	sb.append(" --dst_path ");
	sb.append(dstPath);
	sb.append(" --owner vcap:vcap");

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private void run(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    List<ScriptAction> scripts) throws IOException, TimeoutException,
	    InterruptedException {

	logger.println("Run script in Warden Container");
	for (ScriptAction script : scripts) {
	    run(handle, build, launcher, listener, script);
	}
    }

    private boolean run(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    ScriptAction script) throws IOException, TimeoutException,
	    InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	scriptFile = generateScript(build, launcher, listener,
		script.getScript());
	// String absolutePath =
	// scriptFile.toURI().toString().split("file:")[1];
	copyIn(handle, build, launcher, listener, "" + scriptFile, "/tmp");
	String scriptName = scriptFile.getName();

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- run ");
	sb.append(" --handle ");
	sb.append(handle);
	sb.append(" --script ");
	sb.append("\"chmod +x /tmp/" + scriptName + "\"\n");
	sb.append(warden_repl_v2);
	sb.append(" -- run ");
	sb.append(" --handle ");
	sb.append(handle);
	sb.append(" --script ");
	sb.append("\"/tmp/" + scriptName + "\"\n");

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private boolean ignorePassword(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener)
	    throws IOException, TimeoutException, InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	scriptFile = generateScript(build, launcher, listener,
		getIgnorePassword(handle));
	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private boolean propagateEnvironment(String handle,
	    AbstractBuild<?, ?> build, final Launcher launcher,
	    final BuildListener listener, EnvVars envVars,
	    ArrayList<String> variables) throws IOException, TimeoutException,
	    InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	scriptFile = generateScript(build, launcher, listener,
		getPropagateEnvironment(handle, envVars, variables));

	// I don't want to such output displayed in the logger
	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(new ByteArrayOutputStream())
		.stdout(new ByteArrayOutputStream()).pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private boolean limitDisk(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    EnvVars envVars, String volume) throws IOException,
	    TimeoutException, InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	Integer volumeInByte = convertStringToInt(volume);

	if (volumeInByte <= 0)
	    return true;

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- limit_disk ");
	sb.append(" --handle ");
	sb.append(handle);
	sb.append(" --byte_limit ");
	sb.append(volumeInByte);

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private boolean limitMemory(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    EnvVars envVars, String volume) throws IOException,
	    TimeoutException, InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	Integer volumeInByte = convertStringToInt(volume);

	if (volumeInByte <= 0)
	    return true;

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- limit_memory ");
	sb.append(" --handle ");
	sb.append(handle);
	sb.append(" --limit_in_bytes ");
	sb.append(volumeInByte);

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }

    private boolean limitBandwidth(String handle, AbstractBuild<?, ?> build,
	    final Launcher launcher, final BuildListener listener,
	    EnvVars envVars, String volume) throws IOException,
	    TimeoutException, InterruptedException {

	FilePath ws = build.getWorkspace(), scriptFile;

	Integer volumeInByte = convertStringToInt(volume);

	if (volumeInByte <= 0)
	    return true;

	StringBuilder sb = new StringBuilder();

	sb.append(getEnvironment());
	sb.append(warden_repl_v2);
	sb.append(" -- limit_bandwidth ");
	sb.append(" --handle ");
	sb.append(handle);
	sb.append(" --rate ");
	sb.append(volumeInByte);

	scriptFile = generateScript(build, launcher, listener, sb.toString());

	int returnCode = launcher.launch().cmds(buildCommandLine(scriptFile))
		.envs(build.getEnvironment(listener))
		.stderr(listener.getLogger()).stdout(listener.getLogger())
		.pwd(ws).join();

	if (scriptFile.exists()) {
	    scriptFile.delete();
	}

	if (returnCode != 0) {
	    listener.fatalError(Messages
		    .EnvironmentScriptWrapper_UnableToExecuteScript(returnCode));
	    return false;
	}

	return true;
    }
}
