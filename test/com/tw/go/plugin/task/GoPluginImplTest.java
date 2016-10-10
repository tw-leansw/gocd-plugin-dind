package com.tw.go.plugin.task;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GoPluginImplTest {
    @Test
    public void shouldGetCorrectDockerCmd() {
        GoPluginImpl plugin = new GoPluginImpl();

        assertThat(plugin.getDockerCmd("/work", "maven 3", "mvn clean install -s settings.xml"),
                is("docker run --rm -v /work:/workspace -w /workspace -v /root/.m2:/root/.m2 registry.cn-hangzhou.aliyuncs.com/leansw/maven:3.3-jdk-8 sh -c 'mvn clean install -s settings.xml'"));

        assertThat(plugin.getDockerCmd("/work", "nodejs 6", "npm install --registry https://registry.npm.taobao.org"),
                is("docker run --rm -v /work:/workspace -w /workspace  registry.cn-hangzhou.aliyuncs.com/leansw/node:6.7.0 sh -c 'npm install --registry https://registry.npm.taobao.org'"));

    }
}