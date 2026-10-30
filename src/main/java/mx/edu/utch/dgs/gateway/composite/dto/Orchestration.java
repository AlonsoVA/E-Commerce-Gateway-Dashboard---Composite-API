package mx.edu.utch.dgs.gateway.composite.dto;

import java.util.List;

public record Orchestration(String strategy, long elapsedMs, List<CallTrace> calls) {
}
