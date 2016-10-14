package com.tw.go.plugin.task;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GoPluginImplTest {
    @Test
    public void shouldGetCorrectDockerCmd() {
        GoPluginImpl plugin = new GoPluginImpl();

        assertThat(plugin.getDockerParam("pipelines/demo", "maven3", "", false),
                is("docker run --rm -v /run/go-agent/pipelines/demo:/workspace -w /workspace -v /root/.m2:/root/.m2 registry.cn-hangzhou.aliyuncs.com/leansw/maven:3.3-jdk-8 sh -c "));

        assertThat(plugin.getDockerParam("pipelines/demo", "node6", "NODE_HOME=/home/node",false),
                is("docker run --rm -v /run/go-agent/pipelines/demo:/workspace -w /workspace  -e NODE_HOME=/home/node registry.cn-hangzhou.aliyuncs.com/leansw/node:6.7.0 sh -c "));

        assertThat(plugin.getDockerParam("pipelines/demo", "node6", "VAR1=VAL1;VAR2=VAL2",true),
                is("docker run --rm -v /run/go-agent/pipelines/demo:/workspace -w /workspace  -e VAR1=VAL1 -e VAR2=VAL2 node6 sh -c "));
    }
}