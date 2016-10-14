/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.tw.go.plugin.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Extension
public class GoPluginImpl implements GoPlugin {
    public final static String EXTENSION_NAME = "task";
    public final static List<String> SUPPORTED_API_VERSIONS = asList("1.0");

    public final static String REQUEST_CONFIGURATION = "configuration";
    public final static String REQUEST_CONFIGURATION_2 = "go.plugin-settings.get-configuration";
    public final static String REQUEST_VALIDATION = "validate";
    public final static String REQUEST_VALIDATION_2 = "go.plugin-settings.validate-configuration";
    public final static String REQUEST_TASK_VIEW = "view";
    public final static String REQUEST_TASK_VIEW_2 = "go.plugin-settings.get-view";
    public final static String REQUEST_EXECUTION = "execute";

    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final String WORKDIR_PREFIX = "/run/go-agent/";

    private static Logger LOGGER = Logger.getLoggerFor(GoPluginImpl.class);

    private Map<String, String> dockerImageMap = ImmutableMap.of(
            "maven3", "registry.cn-hangzhou.aliyuncs.com/leansw/maven:3.3-jdk-8",
            "node6", "registry.cn-hangzhou.aliyuncs.com/leansw/node:6.7.0");
    private Map<String, String> extraParametersMap = ImmutableMap.of(
            "maven3", "-v /root/.m2:/root/.m2",
            "node6", "");

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        // ignore
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) throws UnhandledRequestTypeException {
        if (goPluginApiRequest.requestName().equals(REQUEST_CONFIGURATION) || goPluginApiRequest.requestName().equals(REQUEST_CONFIGURATION_2)) {
            return handleConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATION) || goPluginApiRequest.requestName().equals(REQUEST_VALIDATION_2)) {
            return handleValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_TASK_VIEW) || goPluginApiRequest.requestName().equals(REQUEST_TASK_VIEW_2)) {
            try {
                return handleView();
            } catch (IOException e) {
                String message = "Failed to find template: " + e.getMessage();
                return renderJSON(500, message);
            }
        } else if (goPluginApiRequest.requestName().equals(REQUEST_EXECUTION)) {
            return handleExecute(goPluginApiRequest);
        }
        return null;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier(EXTENSION_NAME, SUPPORTED_API_VERSIONS);
    }

    private GoPluginApiResponse handleConfiguration() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("source", createField("ImageSource", "std", true, false, "0"));
        response.put("stdImg", createField("StandardImage", "maven3", false, false, "1"));
        response.put("diyImg", createField("UserImage", "", false, false, "2"));
        response.put("script", createField("BuildScript", "", true, false, "3"));
        response.put("envs", createField("Environments", "", false, false, "4"));
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private Map<String, Object> createField(String displayName, String defaultValue, boolean isRequired, boolean isSecure, String displayOrder) {
        Map<String, Object> fieldProperties = new HashMap<String, Object>();
        fieldProperties.put("display-name", displayName);
        fieldProperties.put("default-value", defaultValue);
        fieldProperties.put("required", isRequired);
        fieldProperties.put("secure", isSecure);
        fieldProperties.put("display-order", displayOrder);
        return fieldProperties;
    }

    private GoPluginApiResponse handleValidation(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> response = new HashMap<String, Object>();
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleView() throws IOException {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("displayValue", "Docker Task Executor");
        response.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8"));
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleExecute(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> response = new HashMap<String, Object>();
        try {
            Map<String, Object> request = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);

            Map<String, Object> configKeyValuePairs = (Map<String, Object>) request.get("config");
            Map<String, Object> context = (Map<String, Object>) request.get("context");
            String workingDirectory = (String) context.get("workingDirectory");
            Map<String, String> environmentVariables = (Map<String, String>) context.get("environmentVariables");

            Map<String, String> imageSourceConfig = (Map<String, String>) configKeyValuePairs.get("source");
            String imageSource = imageSourceConfig.get("value");
            Map<String, String> standardImageConfig = (Map<String, String>) configKeyValuePairs.get("stdImg");
            String stdImage = standardImageConfig.get("value");
            Map<String, String> diyImageConfig = (Map<String, String>) configKeyValuePairs.get("diyImg");
            String diyImage = diyImageConfig.get("value");
            String buildImage = imageSource.equals("std") ? stdImage : diyImage;
            Map<String, String> scriptConfig = (Map<String, String>) configKeyValuePairs.get("script");
            String buildScript = scriptConfig.get("value");
            Map<String, String> envsConfig = (Map<String, String>) configKeyValuePairs.get("envs");
            String envVars = envsConfig.get("value").replaceAll("\n", ";");

            JobConsoleLogger.getConsoleLogger().printLine("[docker-executor] -------------------------");
            JobConsoleLogger.getConsoleLogger().printLine("[docker-executor] Build Image: " + buildImage);
            JobConsoleLogger.getConsoleLogger().printLine("[docker-executor] Build Script: " + buildScript);
            JobConsoleLogger.getConsoleLogger().printLine("[docker-executor] Extra EnvVars: " + envVars);
            JobConsoleLogger.getConsoleLogger().printLine("[docker-executor] -------------------------");

            String dockerParam = getDockerParam(workingDirectory, buildImage, envVars, imageSource.equals("diy"));
            JobConsoleLogger.getConsoleLogger().printLine("[docker-executor] " + dockerParam + "'" + buildScript + "'");
            int exitCode = executeDockerCommand(workingDirectory, environmentVariables, dockerParam, buildScript);

            if (exitCode == 0) {
                response.put("success", true);
                response.put("message", "[docker-executor] Build completed successfully.");
            } else {
                response.put("success", false);
                response.put("message", "[docker-executor] Build completed with exit code: " + exitCode + ".");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "[docker-executor] Build execution interrupted. Reason: " + e.getMessage());
        }
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    protected String getDockerParam(String workingDirectory, String buildImage, String envVars, boolean isDIYImage) {
        String dockerImageName, extraParameters;
        if (isDIYImage) {
            dockerImageName = buildImage;
            extraParameters = "";
        } else {
            dockerImageName = dockerImageMap.get(buildImage);
            extraParameters = extraParametersMap.get(buildImage);
        }
        String[] varList = envVars.split(";");
        for (String var : varList) {
            if (var.indexOf("=") > 0) {
                extraParameters += (" -e " + var.trim());
            }
        }
        String dockerCmd = "docker run --rm -v " + WORKDIR_PREFIX + workingDirectory + ":/workspace -w /workspace " +
                extraParameters + " " + dockerImageName + " sh -c ";
        return dockerCmd;
    }

    private int executeDockerCommand(String workingDirectory, Map<String, String> environmentVariables,
                                     String dockerParam, String buildScript) throws IOException, InterruptedException {
        List<String> parameters = Lists.newArrayList(dockerParam.split("[ ]+"));
        parameters.add(buildScript);
        return executeCommand(workingDirectory, environmentVariables, parameters.toArray(new String[parameters.size()]));
    }

    private int executeCommand(String workingDirectory, Map<String, String> environmentVariables, String... command)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(workingDirectory));
        if (environmentVariables != null && !environmentVariables.isEmpty()) {
            processBuilder.environment().putAll(environmentVariables);
        }
        Process process = processBuilder.start();

        JobConsoleLogger.getConsoleLogger().readOutputOf(process.getInputStream());
        JobConsoleLogger.getConsoleLogger().readErrorOf(process.getErrorStream());

        return process.waitFor();
    }

    private GoPluginApiResponse renderJSON(final int responseCode, Object response) {
        final String json = response == null ? null : new GsonBuilder().create().toJson(response);
        return new GoPluginApiResponse() {
            @Override
            public int responseCode() {
                return responseCode;
            }

            @Override
            public Map<String, String> responseHeaders() {
                return null;
            }

            @Override
            public String responseBody() {
                return json;
            }
        };
    }
}
