package io.jenkins.plugins;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.TestNotifier;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

//TODO : Need to verify the test cases
public class BasicApiPluginTest {

    private static final String filePattern = "*.xml";
    private static final boolean fileFound = Boolean.TRUE;

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConfigRoundtrip() {
        rr.then(jenkins -> {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.getPublishersList().add(new BasicApiPlugin(filePattern, fileFound));
            project = jenkins.configRoundtrip(project);

            jenkins.assertEqualDataBoundBeans(new BasicApiPlugin(filePattern, fileFound), project.getPublishersList().get(0));
        });
    }

    /**
     * To check Config properties set properly or not
     */
    @Test
    public void configPageTest() {
        rr.then(jenkins -> {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            BasicApiPlugin basicApiPluginBefore = new BasicApiPlugin(filePattern, fileFound);
            project.getPublishersList().add(basicApiPluginBefore);
            jenkins.submit(jenkins.createWebClient().getPage(project, "configure").getFormByName("config"));
            BasicApiPlugin basicApiPluginAfter = project.getPublishersList().get(BasicApiPlugin.class);
            jenkins.assertEqualBeans(basicApiPluginBefore, basicApiPluginAfter, "failBuildIfNoFiles,fileSearchPattern");
        });
    }

    /**
     * To Check Global config persist after jenkins restart
     */

    @Test
    public void globalConfigCheck() {

        rr.then(r -> {
            BasicApiPlugin.DescriptorImpl descriptoriml = (BasicApiPlugin.DescriptorImpl) r.jenkins.getDescriptor(BasicApiPlugin.class);
            Method templatesField = ReflectionUtils.getPublicMethodNamed(BasicApiPlugin.DescriptorImpl.class, "getFilesUploadApiUrl");
            assertNull("not set initially", templatesField.getDefaultValue());
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName("_.filesUploadApiUrl");
            textbox.setText("hello");
            r.submit(config);
            assert descriptoriml != null;
            assertEquals("global config page let us edit it", "hello", descriptoriml.getFilesUploadApiUrl());
        });
        rr.then(r -> {
            BasicApiPlugin.DescriptorImpl descriptorIml = (BasicApiPlugin.DescriptorImpl) r.jenkins.getDescriptor(BasicApiPlugin.class);
            assert descriptorIml != null;
            assertEquals("still there after restart of Jenkins", "hello", descriptorIml.getFilesUploadApiUrl());
        });
    }

    /**
     * To Check the build Result
     */

    @Test
    public void testBuildFilesWithCheckBoxChecked() {
        rr.then(jenkins -> {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            BasicApiPlugin basicApiPlugin = new BasicApiPlugin("*.xml", Boolean.TRUE);
            project.getPublishersList().add(basicApiPlugin);
            Objects.requireNonNull(project.scheduleBuild2(0).get().getWorkspace()).createTextTempFile("pom", ".xml", "hello");
            project.getPublishersList().add(new TestNotifier() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                    listener.getLogger().println("OK");
                    return true;
                }
            });
            FreeStyleBuild buildResult = jenkins.buildAndAssertSuccess(project);
            jenkins.assertLogContains("Starting", buildResult);
            jenkins.assertLogContains("OK", buildResult);

        });
    }

    @Test
    public void getFilePattern() {
        String testFilePattern = "*.xml";
        assertEquals(testFilePattern, new BasicApiPlugin(filePattern, fileFound).getFileSearchPattern());
    }

}