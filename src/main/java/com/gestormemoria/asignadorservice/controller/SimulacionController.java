package com.gestormemoria.asignadorservice.controller;

import com.gestormemoria.asignadorservice.dto.SimulacionRequestDTO;
import com.gestormemoria.asignadorservice.model.EstrategiaEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/simular")
public class SimulacionController {

    @PostMapping
    public ResponseEntity> simular(@RequestBody SimulacionRequestDTO dto) {
        EstrategiaEnum estrategiaEnum = EstrategiaEnum.deCodigo(dto.getEstrategia());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mapaId", dto.getMapaId());
        respuesta.put("secuenciaId", dto.getSecuenciaId());
        respuesta.put("estrategia", estrategiaEnum.name());
        respuesta.put("codigoEstrategia", estrategiaEnum.getCodigo());
        respuesta.put("estado", "PREPARADA");
        respuesta.put("mensaje", "La corrida fue configurada exitosamente.");

        return ResponseEntity.ok(respuesta);
    }
}