import javafx.application.Application;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {


        DataRepository repo = new DataRepository();
        CsvImportService importService = new CsvImportService(repo);

        try {

            //  CSV IMPORT
            importService.importAll(
                    Paths.get("sampleData_AllStudents.csv"),
                    Paths.get("sampleData_AllCourses.csv"),
                    Paths.get("sampleData_AllClassroomsAndTheirCapacities.csv"),
                    Paths.get("sampleData_AllAttendanceLists.csv"),
                    Paths.get("sampleData_slot_config.csv")
            );

            System.out.println("Courses: " + repo.getCourses().size());
            System.out.println("Students: " + repo.getStudents().size());
            System.out.println("Classrooms: " + repo.getClassrooms().size());
            System.out.println("Slots: " + repo.getSlots().size());

            // SCHEDULING ENGINE
            SchedulingEngine engine = new SchedulingEngine(repo);

            try {

                Schedule schedule = engine.generateExamSchedule();

                // EXPORT
                CsvExportService exportService = new CsvExportService(repo);
                exportService.exportByCourse(schedule, Paths.get("schedule_by_course.csv"));
                exportService.exportByRoom(schedule, Paths.get("schedule_by_room.csv"));
                exportService.exportByStudent(schedule, Paths.get("schedule_by_student.csv"));
                exportService.exportByDaySlot(schedule, Paths.get("schedule_by_day_slot.csv"));

                System.out.println("Export completed.");

                StudentScheduleService studentScheduleService =
                        new StudentScheduleService(repo);


                for (String studentId : repo.getStudents().keySet()) {
                    studentScheduleService.printScheduleForStudent(studentId, schedule);
                    System.out.println();
                }


            } catch (RuntimeException e) {


                System.out.println("No feasible schedule found.");
                System.out.println("Suggested relaxations:");

                for (RelaxationSuggestion r : engine.suggestRelaxations()) {
                    System.out.println(
                            r.getType() + " → " + r.getExplanation()
                    );
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error while importing/exporting CSV data", e);
        }

    }
}
