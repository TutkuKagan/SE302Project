package com.example.scheduler.controller;

import com.example.scheduler.model.DataRepository;
import com.example.scheduler.model.Schedule;
import com.example.scheduler.model.Slot;
import com.example.scheduler.model.SlotConfigurationRow;
import com.example.scheduler.service.SchedulingEngine;
import com.example.scheduler.service.SlotGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SlotController {

    private final DataRepository repo;
    private final ObservableList<SlotConfigurationRow> slotList;

    public SlotController(DataRepository repo) {
        this.repo = repo;
        this.slotList = FXCollections.observableArrayList();
    }

    public ObservableList<SlotConfigurationRow> getSlotList() {
        return slotList;
    }

    public void loadInitialData() {
        slotList.clear();
        List<Slot> slots = repo.getSlots();

        if (slots == null || slots.isEmpty()) {
            // Default
            slotList.add(new SlotConfigurationRow(1, 1, "09:00", "11:00"));
            slotList.add(new SlotConfigurationRow(1, 2, "14:00", "16:00"));
            slotList.add(new SlotConfigurationRow(1, 3, "19:00", "21:00"));
            return;
        }

        Map<Integer, String> indexToRange = new TreeMap<>();
        for (Slot s : slots) {
            indexToRange.putIfAbsent(s.getIndex(), s.getTimeRange());
        }

        for (Map.Entry<Integer, String> entry : indexToRange.entrySet()) {
            int slotIndex = entry.getKey();
            String range = entry.getValue();

            String[] parts = range.split("-");
            String start = parts.length > 0 ? parts[0].trim() : "";
            String end = parts.length > 1 ? parts[1].trim() : "";

            slotList.add(new SlotConfigurationRow(1, slotIndex, start, end));
        }
    }

    public int calculateMaxDay() {
        if (repo.getSlots() == null || repo.getSlots().isEmpty())
            return 5;
        return repo.getSlots().stream().mapToInt(Slot::getDay).max().orElse(5);
    }

    public void addSlot() {
        int nextSlotIndex = slotList.stream()
                .mapToInt(SlotConfigurationRow::getSlotIndex)
                .max().orElse(0) + 1;
        slotList.add(new SlotConfigurationRow(1, nextSlotIndex, "00:00", "00:00"));
    }

    public Schedule saveConfiguration(File file, int numDays) throws Exception {
        if (slotList.isEmpty()) {
            throw new Exception("At least one slot must be defined.");
        }

        List<SlotConfigurationRow> rows = new ArrayList<>(slotList);
        rows.sort(Comparator.comparingInt(SlotConfigurationRow::getSlotIndex));

        List<String> timeRanges = new ArrayList<>();
        for (SlotConfigurationRow row : rows) {
            String startTime = row.getStartTime().trim();
            String endTime = row.getEndTime().trim();
            if (startTime.isEmpty() || endTime.isEmpty()) {
                throw new Exception("Start and end times must be provided for all slots.");
            }
            timeRanges.add(startTime + "-" + endTime);
        }

        // Update Repo
        List<Slot> newSlots = SlotGenerator.generateSlots(numDays, timeRanges);
        repo.setSlots(newSlots);

        // Write to File
        if (file != null) {
            try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                bw.write(Integer.toString(numDays));
                for (String tr : timeRanges) {
                    bw.write(";");
                    bw.write(tr);
                }
                bw.newLine();
            }
        }

        // Reschedule
        if (!repo.getCourses().isEmpty() && !repo.getClassrooms().isEmpty() && !repo.getSlots().isEmpty()) {
            return new SchedulingEngine(repo).generateExamSchedule();
        }
        return null;
    }
}
