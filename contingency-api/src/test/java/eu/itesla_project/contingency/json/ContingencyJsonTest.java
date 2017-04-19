package eu.itesla_project.contingency.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.itesla_project.contingency.*;
import org.junit.Test;

import eu.itesla_project.commons.ConverterBaseTest;

public class ContingencyJsonTest extends ConverterBaseTest {
    
    private static Contingency create() {
        List<ContingencyElement> elements = new ArrayList<>();
        elements.add(new BranchContingency("NHV1_NHV2_2", "P1"));
        elements.add(new BranchContingency("NHV1_NHV2_1"));
        elements.add(new GeneratorContingency("GEN"));

        return new ContingencyImpl("contingency", elements);
    }

    private static Contingency read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Contingency.class, new ContingencyDeserializer());
            module.addDeserializer(ContingencyElement.class, new ContingencyElementDeserializer());
            objectMapper.registerModule(module);

            return objectMapper.readValue(is, Contingency.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void write(Contingency object, Path jsonFile) {
        Objects.requireNonNull(object);
        Objects.requireNonNull(jsonFile);

        try (OutputStream os = Files.newOutputStream(jsonFile)) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

            writer.writeValue(os, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void roundTripTest() throws IOException {
        roundTripTest(create(), ContingencyJsonTest::write, ContingencyJsonTest::read, "/contingency.json");
    }
}