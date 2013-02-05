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
public class CopyInAction extends AbstractDescribableImpl<CopyInAction> {

    private String srcPath;

    private String destPath;

    @DataBoundConstructor
    public CopyInAction(String srcPath, String destPath) {

	super();
	this.srcPath = srcPath;
	this.destPath = destPath;
    }

    public String getSrcPath() {
	return srcPath;
    }

    public String getDestPath() {
	return destPath;
    }

    // TODO: need more enhancement here, currently only check the length
    public Boolean isWell() {
	if (srcPath == null || 0 == srcPath.trim().length() || destPath == null
		|| 0 == destPath.trim().length()) {
	    return false;
	}

	return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<CopyInAction> {

	@Override
	public String getDisplayName() {
	    return "Copy In";
	}

    }

}