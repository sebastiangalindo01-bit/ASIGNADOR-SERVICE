package com.gestormemoria.asignadorservice.model;

public enum EstrategiaEnum {
    FIRST_FIT(0),
    BEST_FIT(1),
    WORST_FIT(2);

    private final int codigo;

    EstrategiaEnum(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static EstrategiaEnum deCodigo(int codigo) {
        for (EstrategiaEnum e : values()) {
            if (e.getCodigo() == codigo) {
                return e;
            }
        }
        throw new IllegalArgumentException("Código de estrategia no válido: " + codigo);
    }
}