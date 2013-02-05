package org.cloudfoundry.ci.plugins.warden;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 
 * @author <a href="mailto:jli@vmware.com">Jacky Li</a>
 * 
 */
public class ScriptAction extends AbstractDescribableImpl<ScriptAction> {

    private String script;

    @DataBoundConstructor
    public ScriptAction(String script) {

	super();
	this.script = script;
    }

    public String getScript() {
	return script;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<ScriptAction> {

	@Override
	public String getDisplayName() {
	    return "Warden Script";
	}

    }

}