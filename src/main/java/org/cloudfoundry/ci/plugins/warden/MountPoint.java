package org.cloudfoundry.ci.plugins.warden;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.IOException;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * 
 * @author <a href="mailto:jli@vmware.com">Jacky Li</a>
 * 
 */
public class MountPoint extends AbstractDescribableImpl<MountPoint> {

    private String srcPath;

    private String destPath;

    private String mntMode;

    @DataBoundConstructor
    public MountPoint(String srcPath, String destPath, String mntMode) {

	super();
	this.srcPath = srcPath;
	this.destPath = destPath;
	this.mntMode = mntMode;
    }

    public String getSrcPath() {
	return srcPath;
    }

    public String getDestPath() {
	return destPath;
    }

    public String getMntMode() {
	return mntMode;
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
    public static final class DescriptorImpl extends Descriptor<MountPoint> {

	@Override
	public String getDisplayName() {
	    return "Mount Point";
	}

	public FormValidation doCheckSrcPath(
		@AncestorInPath AbstractProject<?, ?> project,
		@QueryParameter String value) throws IOException {
	    if (0 == value.length()) {
		return FormValidation.error("Empty Source Path");
	    }
	    return FormValidation.ok();
	}

	public FormValidation doCheckDestPath(
		@AncestorInPath AbstractProject<?, ?> project,
		@QueryParameter String value) throws IOException {
	    if (0 == value.length()) {
		return FormValidation.error("Empty Destination Path");
	    }
	    return FormValidation.ok();
	}

    }

}