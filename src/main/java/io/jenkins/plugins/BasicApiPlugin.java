package io.jenkins.plugins;

import com.google.common.collect.Lists;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class BasicApiPlugin extends Notifier {

    private final String fileSearchPattern;
    private final boolean failBuildIfNoFiles;

    @DataBoundConstructor
    public BasicApiPlugin(final String fileSearchPattern, final boolean failBuildIfNoFiles) {
        this.fileSearchPattern = fileSearchPattern;
        this.failBuildIfNoFiles = failBuildIfNoFiles;
    }


    public String getFileSearchPattern() {
        return fileSearchPattern;
    }

    public boolean getFailBuildIfNoFiles() {
        return failBuildIfNoFiles;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        try {
            listener.getLogger().println("Starting Post Build Action");
            FilePath workspace = Objects.requireNonNull(build.getWorkspace());
            if (workspace.isRemote()) {
                workspace = new FilePath(workspace.getChannel(), workspace.getRemote());
            }

            List<String> searchResult = this.searchFiles(workspace);
            if (searchResult.isEmpty() && failBuildIfNoFiles) {
                listener.getLogger().println("No Files Found!");
                build.setResult(Result.FAILURE);
                return Boolean.FALSE;

            } else if (searchResult.isEmpty()) {
                listener.fatalError("No Files Found!");
                build.setResult(Result.UNSTABLE);
                return Boolean.TRUE;

            } else if (StringUtils.isNotBlank(this.getDescriptor().getFilesUploadApiUrl())) {
                //TODO : Need to verify API calling and multipart data binding for request
                MultipartUtility multipart = new MultipartUtility(this.getDescriptor().getFilesUploadApiUrl(), "UTF-8");

                for (String file : searchResult) {
                    multipart.addFilePart("fileUpload", new File(workspace.getRemote().concat(File.pathSeparator).concat(file)));
                }

                List<String> response = multipart.finish();
                response.forEach(line -> listener.getLogger().println(line));
            }

            listener.getLogger().println("Finished Post Build Action");
            build.setResult(Result.SUCCESS);
            return Boolean.TRUE;
        } catch (Exception ex) {
            listener.error("Error occurred while performing Post Build Action", ex);
            build.setResult(Result.FAILURE);
            return Boolean.FALSE;
        }
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Serach the current job workspace for files according to given file pattern (ant pattern)
     *
     * @param workspace : File path of current job workspace
     * @return : files path
     */
    private List<String> searchFiles(FilePath workspace) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setIncludes(new String[]{this.getFileSearchPattern()});
        directoryScanner.setBasedir(workspace.getRemote());
        directoryScanner.setCaseSensitive(Boolean.TRUE);
        directoryScanner.scan();
        return Lists.newArrayList(directoryScanner.getIncludedFiles());
    }


}