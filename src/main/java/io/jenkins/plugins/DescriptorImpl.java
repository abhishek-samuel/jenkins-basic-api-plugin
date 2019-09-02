package io.jenkins.plugins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private static final String PLUGIN_NAME = "Custom Basic Api plugin";
    private static final String API_URL = "filesUploadApiUrl";

    private String filesUploadApiUrl;

    public DescriptorImpl() {
        this.load();
    }

    public String getFilesUploadApiUrl() {
        return filesUploadApiUrl;
    }

    /**
     * Save the Global Config
     *
     * @param req
     * @param formData
     * @return Boolean
     * @throws FormException
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws FormException {
        filesUploadApiUrl = formData.getString(API_URL);
        req.bindJSON(this, formData);
        this.save();
        return super.configure(req, formData);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return Boolean.TRUE;
    }

    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }


    /**
     * For Validation Of File Pattern Field In Config Properties
     *
     * @param filePattern : fileSearchPattern input text
     * @return FormValidation
     */
    public FormValidation doCheckFilePattern(@QueryParameter String filePattern) {
        if (filePattern.length() == 0)
            return FormValidation.error("Please input correct pattern");
        if (filePattern.trim().isEmpty())
            return FormValidation.error("Please input correct pattern");
        if (filePattern.length() < 4)
            return FormValidation.warning("Isn't the pattern too short?");
        return FormValidation.ok();
    }

    /**
     * For Validation Of Server Url Field In Global Properties
     *
     * @param serverUrl : custom server url
     * @return FormValidation
     */
    public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {
        if (serverUrl.length() == 0)
            return FormValidation.error("Please input valid server url");
        if (serverUrl.trim().isEmpty())
            return FormValidation.error("Please input valid server url");
        if (serverUrl.length() < 4)
            return FormValidation.warning("Isn't the url too short?");
        return FormValidation.ok();
    }

}
