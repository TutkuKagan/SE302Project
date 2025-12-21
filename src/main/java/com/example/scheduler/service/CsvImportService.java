package com.example.scheduler.service;

import com.example.scheduler.model.DataRepository;
import java.io.IOException;
import java.nio.file.Path;

public class CsvImportService {

    private final DataRepository repository;

    public CsvImportService(DataRepository repository) {
        this.repository = repository;
    }

    public void importAll(Path studentsCsv,
            Path coursesCsv,
            Path classroomsCsv,
            Path registrationsCsv,
            Path slotsCsv) throws IOException {

        repository.loadAll(studentsCsv, coursesCsv, classroomsCsv, registrationsCsv);
        repository.loadSlots(slotsCsv);
    }
}
