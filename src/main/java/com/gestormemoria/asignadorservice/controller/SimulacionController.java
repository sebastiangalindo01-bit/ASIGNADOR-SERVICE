
package com.gestormemoria.asignadorservice.controller;

import com.gestormemoria.asignadorservice.dto.SimulacionRequestDTO;
import com.gestormemoria.asignadorservice.dto.SimulacionResponseDTO;
import com.gestormemoria.asignadorservice.service.SimulacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/simular")
public class SimulacionController {

    private final SimulacionService simulacionService;

    public SimulacionController(SimulacionService simulacionService) {
        this.simulacionService = simulacionService;
    }

    @PostMapping
    public ResponseEntity<?> simular(
            @RequestBody SimulacionRequestDTO dto) {

        try {
            SimulacionResponseDTO respuesta =
                    simulacionService.ejecutarNativo(dto);

            return ResponseEntity.ok(respuesta);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "estado", "ERROR",
                            "mensaje", e.getMessage()
                    )
            );
        }
    }
}
