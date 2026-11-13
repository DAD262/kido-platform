package pe.edu.upeu.curso.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upeu.curso.dto.CursoResponse;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CursoPortadaStorageServiceTest {
    @TempDir Path temporal;
    private CursoService cursos;
    private CursoPortadaStorageService storage;

    @BeforeEach
    void preparar() {
        cursos = mock(CursoService.class);
        storage = new CursoPortadaStorageService(cursos);
        ReflectionTestUtils.setField(storage, "portadasDir", temporal.toString());
        ReflectionTestUtils.setField(storage, "maxBytes", 25L * 1024 * 1024);
    }

    @Test
    void guardaPortadaSoloParaCursoPublicado() throws Exception {
        when(cursos.obtener(18L)).thenReturn(curso("PUBLICADO", null));
        when(cursos.actualizarPortada(eq(18L), anyString()))
                .thenAnswer(inv -> curso("PUBLICADO", inv.getArgument(1)));
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        var archivo = new MockMultipartFile("archivo", "portada.png", "image/png", png);

        CursoResponse resultado = storage.guardar(18L, archivo);

        assertNotNull(resultado.portadaUrl());
        assertTrue(resultado.portadaUrl().startsWith("/api/v1/cursos/portadas/curso-18-"));
        String nombre = resultado.portadaUrl().substring(resultado.portadaUrl().lastIndexOf('/') + 1);
        assertTrue(Files.exists(temporal.resolve(nombre)));
        assertTrue(storage.cargar(nombre).exists());
    }

    @Test
    void rechazaPortadaMientrasCursoNoEstePublicado() {
        when(cursos.obtener(18L)).thenReturn(curso("PENDIENTE_REVISION", null));
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        var archivo = new MockMultipartFile("archivo", "portada.png", "image/png", png);

        var error = assertThrows(IllegalStateException.class, () -> storage.guardar(18L, archivo));

        assertTrue(error.getMessage().contains("administrador"));
        verify(cursos, never()).actualizarPortada(anyLong(), any());
    }

    private CursoResponse curso(String estado, String portada) {
        return new CursoResponse(18L, "Curso", "Descripción", "GRATUITO", BigDecimal.ZERO,
                1L, estado, 1L, "Tecnología", null, null, null, portada);
    }
}
