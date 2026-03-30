package com.github.fiodarks.project26.audit.application.port.in;

public interface RecordAuditEventUseCase {
    void record(RecordAuditEventCommand command);
}

