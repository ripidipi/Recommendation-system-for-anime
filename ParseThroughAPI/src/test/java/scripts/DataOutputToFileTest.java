package scripts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import utils.SimpleDataExtract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataOutputToFileTest {

    private final InputStream systemIn = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(systemIn);
    }

    @Test
    void extractUsersToFile_whenFilesAbsent_exportsBoth() throws Exception {
        try (MockedConstruction<SimpleDataExtract> sc = mockConstruction(SimpleDataExtract.class)) {
            Path base = Files.createTempDirectory("dataoutputtest_absent");
            File outDir = base.resolve("out").toFile();
            DataOutputToFile instance = new DataOutputToFile(new java.util.Properties());

            instance.extractUsersToFile(outDir);

            assertEquals(1, sc.constructed().size());
            SimpleDataExtract created = sc.constructed().getFirst();

            verify(created, times(2)).exportQueryToParquet(anyString(), any(), any(File.class), any());
            File animeFile = new File(outDir, "anime.parquet");
            File evalFile = new File(outDir, "evaluations.parquet");
            assertNotNull(animeFile.getName());
            assertNotNull(evalFile.getName());
        }
    }

    @Test
    void extractUsersToFile_existingFiles_userSkipsBoth_noExport() throws Exception {
        try (MockedConstruction<SimpleDataExtract> sc = mockConstruction(SimpleDataExtract.class)) {
            Path base = Files.createTempDirectory("dataoutputtest_exist_skip");
            File outDir = base.toFile();
            Files.createDirectories(outDir.toPath());

            File animeFile = new File(outDir, "anime.parquet");
            File evalFile = new File(outDir, "evaluations.parquet");
            assertTrue(animeFile.createNewFile());
            assertTrue(evalFile.createNewFile());

            System.setIn(new ByteArrayInputStream("y\ny\n".getBytes()));

            DataOutputToFile instance = new DataOutputToFile(new java.util.Properties());

            instance.extractUsersToFile(outDir);

            SimpleDataExtract created = sc.constructed().getFirst();
            verify(created, never()).exportQueryToParquet(anyString(), any(), any(File.class), any());
        }
    }

    @Test
    void extractUsersToFile_existingFiles_userOverwrites_exportsAgain() throws Exception {
        try (MockedConstruction<SimpleDataExtract> sc = mockConstruction(SimpleDataExtract.class)) {
            Path base = Files.createTempDirectory("dataoutputtest_exist_overwrite");
            File outDir = base.toFile();
            Files.createDirectories(outDir.toPath());

            File animeFile = new File(outDir, "anime.parquet");
            File evalFile = new File(outDir, "evaluations.parquet");
            assertTrue(animeFile.createNewFile());
            assertTrue(evalFile.createNewFile());

            System.setIn(new ByteArrayInputStream("n\nn\n".getBytes()));

            DataOutputToFile instance = new DataOutputToFile(new java.util.Properties());

            instance.extractUsersToFile(outDir);

            SimpleDataExtract created = sc.constructed().getFirst();
            verify(created, times(2)).exportQueryToParquet(anyString(), any(), any(File.class), any());
        }
    }

    @Test
    void extractUsersToFile_whenFilesCreateDirectoriesThrows_ioHandledAndExportsStillAttempted() throws Exception {
        try (MockedConstruction<SimpleDataExtract> sc = mockConstruction(SimpleDataExtract.class)) {
            DataOutputToFile instance = new DataOutputToFile(new java.util.Properties());

            try (MockedStatic<Files> filesStatic = mockStatic(Files.class)) {
                Path fake = Path.of("/non/writable/dir/should/fail");
                filesStatic.when(() -> Files.createDirectories(any())).thenThrow(new java.io.IOException("nope"));

                File outDir = fake.toFile();

                instance.extractUsersToFile(outDir);

                SimpleDataExtract created = sc.constructed().get(0);
                verify(created, times(2)).exportQueryToParquet(anyString(), any(), any(File.class), any());
            }
        }
    }


    @Test
    void extractUsersToFile_whenExporterThrows_exceptionsAreHandled() throws Exception {
        try (MockedConstruction<SimpleDataExtract> sc = mockConstruction(SimpleDataExtract.class,
                (mock, context) -> {
                    doThrow(new Exception("boom1")).when(mock).exportQueryToParquet(anyString(), any(),
                            argThat(f -> f.getName().equals("anime.parquet")), any());
                    doThrow(new Exception("boom2")).when(mock).exportQueryToParquet(anyString(), any(),
                            argThat(f -> f.getName().equals("evaluations.parquet")), any());
                })) {

            Path base = Files.createTempDirectory("dataoutputtest_exporter_throws");
            File outDir = base.toFile();
            Files.createDirectories(outDir.toPath());

            DataOutputToFile instance = new DataOutputToFile(new java.util.Properties());

            assertDoesNotThrow(() -> instance.extractUsersToFile(outDir));

            SimpleDataExtract created = sc.constructed().getFirst();
            verify(created, times(1))
                    .exportQueryToParquet(anyString(), any(), any(File.class), any());
        }
    }
}
