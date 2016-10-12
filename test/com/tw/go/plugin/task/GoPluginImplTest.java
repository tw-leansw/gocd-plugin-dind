package com.tw.go.plugin.task;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GoPluginImplTest {
    @Test
    public void shouldGetCorrectDockerCmd() {
        GoPluginImpl plugin = new GoPluginImpl();

        assertThat(plugin.getDockerParam("pipelines/demo", "maven3", false),
                is("docker run --rm -v /run/go-agent/pipelines/demo:/workspace -w /workspace -v /root/.m2:/root/.m2 registry.cn-hangzhou.aliyuncs.com/leansw/maven:3.3-jdk-8 sh -c "));

        assertThat(plugin.getDockerParam("pipelines/demo", "node6", false),
                is("docker run --rm -v /run/go-agent/pipelines/demo:/workspace -w /workspace  registry.cn-hangzhou.aliyuncs.com/leansw/node:6.7.0 sh -c "));

        assertThat(plugin.getDockerParam("pipelines/demo", "node6", true),
                is("docker run --rm -v /run/go-agent/pipelines/demo:/workspace -w /workspace  node6 sh -c "));
    }
}