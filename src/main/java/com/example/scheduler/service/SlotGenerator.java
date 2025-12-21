package com.example.scheduler.service;

import com.example.scheduler.model.Slot;

import java.util.ArrayList;
import java.util.List;

public class SlotGenerator {

    // Generates all exam slots for the whole exam period.
    public static List<Slot> generateSlots(int numDays, List<String> timeRanges) {
        List<Slot> result = new ArrayList<>();

        for (int day = 1; day <= numDays; day++) {
            for (int i = 0; i < timeRanges.size(); i++) {
                result.add(new Slot(day, i + 1, timeRanges.get(i)));
            }
        }
        return result;
    }
}
