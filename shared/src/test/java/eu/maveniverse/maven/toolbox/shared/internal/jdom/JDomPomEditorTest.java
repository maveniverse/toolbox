package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import eu.maveniverse.maven.toolbox.shared.internal.PomSuppliers;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

public class JDomPomEditorTest {
    @Test
    void smokeUpdateManagedPlugin() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        try (JDomDocumentIO documentIO =
                new JDomDocumentIO(() -> PomSuppliers.empty400("org.example", "test", "1.0.0-SNAPSHOT"), result::set)) {
            JDomPomEditor.updateManagedPlugin(
                    documentIO.getDocument().getRootElement(), new DefaultArtifact("g:a:v"), true);
        }
        System.out.println(result.get());
    }

    @Test
    void smokeUpdatePlugin() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        try (JDomDocumentIO documentIO =
                new JDomDocumentIO(() -> PomSuppliers.empty400("org.example", "test", "1.0.0-SNAPSHOT"), result::set)) {
            JDomPomEditor.updatePlugin(documentIO.getDocument().getRootElement(), new DefaultArtifact("g:a:v"), true);
        }
        System.out.println(result.get());
    }

    @Test
    void smokeUpdateManagedDependency() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        try (JDomDocumentIO documentIO =
                new JDomDocumentIO(() -> PomSuppliers.empty400("org.example", "test", "1.0.0-SNAPSHOT"), result::set)) {
            JDomPomEditor.updateManagedDependency(
                    documentIO.getDocument().getRootElement(), new DefaultArtifact("g:a:v"), true);
        }
        System.out.println(result.get());
    }

    @Test
    void smokeUpdateDependency() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        try (JDomDocumentIO documentIO =
                new JDomDocumentIO(() -> PomSuppliers.empty400("org.example", "test", "1.0.0-SNAPSHOT"), result::set)) {
            JDomPomEditor.updateDependency(
                    documentIO.getDocument().getRootElement(), new DefaultArtifact("g:a:v"), true);
        }
        System.out.println(result.get());
    }
}
