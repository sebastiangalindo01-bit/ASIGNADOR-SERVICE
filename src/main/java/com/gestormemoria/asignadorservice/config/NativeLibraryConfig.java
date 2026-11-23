package com.gestormemoria.asignadorservice.config;

import com.gestormemoria.asignadorservice.nativebridge.MemoriaNative;
import com.sun.jna.Native;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConditionalOnProperty(name = "asignador.motor", havingValue = "native", matchIfMissing = true)
public class NativeLibraryConfig {

    @Value("${memoria.native.path}")
    private String libraryPath;

    @Bean
    public MemoriaNative memoriaNative() {

        Path path = Paths.get(libraryPath)
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "No se encontró la biblioteca nativa: " + path
            );
        }

        System.out.println(
                "Cargando biblioteca nativa desde: " + path
        );

        return Native.load(path.toString(), MemoriaNative.class);
    }
}
