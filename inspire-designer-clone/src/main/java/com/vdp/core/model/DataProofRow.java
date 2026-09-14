package com.vdp.core.model;

public record DataProofRow(
        String structure,
        String type,
        String value,
        int depth) {
}