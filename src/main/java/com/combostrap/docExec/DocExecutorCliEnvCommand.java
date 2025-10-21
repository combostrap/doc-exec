package com.combostrap.docExec;

import com.combostrap.docExec.jackson.LogLevelSerializer;
import com.combostrap.docExec.jackson.PathSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.Level;

@CommandLine.Command(
        name = "env",
        description = {
                "Show the doc-exec config and derived env",
        },
        mixinStandardHelpOptions = true
)
public class DocExecutorCliEnvCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private DocExecutorCli parent;



    @Override
    public Integer call() {

        DocExecutorInstance instance = DocExecutorCli
                .toDocExecutor(parent)
                .build();

        YAMLFactory yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // Removes "---"
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)         // Minimize quotes
                .build();

        // Create a YAML mapper
        YAMLMapper yamlMapper = new YAMLMapper(yamlFactory);

        // Serializer
        SimpleModule module = new SimpleModule();
        module.addSerializer(Level.class, new LogLevelSerializer());
        module.addSerializer(Path.class, new PathSerializer());
        yamlMapper.registerModule(module);

        // Convert to YAML string
        String yamlString;
        try {
            yamlString = yamlMapper.writeValueAsString(instance);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        System.out.println(yamlString);

        // We throw if any error, so if we come here, it was successful
        return 0;
    }

}
