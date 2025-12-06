import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DataRepository {

    private Map<String, Student> students = new HashMap<>();
    private Map<String, Course> courses = new HashMap<>();
    private List<Classroom> classrooms = new ArrayList<>();
    private List<Slot> slots = new ArrayList<>();
    public List<Slot> getSlots() {
        return slots;
    }

    public void setSlots(List<Slot> slots) {
        this.slots = slots;
    }

    public void loadAll(Path studentsCSV, Path coursesCSV,
                        Path classroomsCSV, Path registrationsCSV) throws IOException {

        // Önce temizleyelim
        students.clear();
        courses.clear();
        classrooms.clear();

        // Students
        for (Student s : loadStudents(studentsCSV)) {
            students.put(s.getStudentId(), s);
        }

        // Courses
        for (Course c : loadCourses(coursesCSV)) {
            courses.put(c.getCourseCode(), c);
        }

        // Classrooms
        classrooms = loadClassrooms(classroomsCSV);

        // Registrations (sampleData_AllAttendanceLists formatına göre)
        Map<String, Course> regMap = loadCourseRegistrations(registrationsCSV);

        // FR3 ile uyumlu merge:
        // Eğer registration'da olup courses CSV'de olmayan ders varsa yine de ekliyoruz.
        for (String courseCode : regMap.keySet()) {
            courses.putIfAbsent(courseCode, new Course(courseCode));
            Course target = courses.get(courseCode);

            for (String stdId : regMap.get(courseCode).getStudentIds()) {
                target.addStudent(stdId);
            }
        }
    }

    public Map<String, Course> getCourses() { return courses; }
    public Map<String, Student> getStudents() { return students; }
    public List<Classroom> getClassrooms() { return classrooms; }

    // =========================
    //   CSV LOAD METOTLARI
    // =========================

    public List<Student> loadStudents(Path path) throws IOException {
        List<Student> list = new ArrayList<>();

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (first) {
                // Örnek: "ALL OF THE STUDENTS IN THE SYSTEM" -> header, atla
                first = false;
                continue;
            }

            list.add(new Student(trimmed));
        }
        return list;
    }

    public List<Course> loadCourses(Path path) throws IOException {
        List<Course> list = new ArrayList<>();

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (first) {
                // Örnek: "ALL OF THE COURSES IN THE SYSTEM" -> header, atla
                first = false;
                continue;
            }

            list.add(new Course(trimmed));
        }
        return list;
    }

    public List<Classroom> loadClassrooms(Path path) throws IOException {
        List<Classroom> list = new ArrayList<>();

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (first) {
                // Örnek: "ALL OF THE CLASSROOMS; AND THEIR CAPACITIES IN THE SYSTEM"
                // -> açıklama satırı, atlıyoruz
                first = false;
                continue;
            }

            String[] parts = trimmed.split(";");
            if (parts.length < 2) {
                System.out.println("⚠ Hatalı classroom satırı: " + line);
                continue;
            }

            String room = parts[0].trim();
            int cap = Integer.parseInt(parts[1].trim());

            list.add(new Classroom(room, cap));
        }
        return list;
    }

    /**
     * sampleData_AllAttendanceLists.csv formatı:
     *
     * CourseCode_01
     * ['Std_ID_170', 'Std_ID_077', ..., 'Std_ID_168']
     *
     * (boş satır)
     * CourseCode_02
     * ['Std_ID_238', 'Std_ID_132', ..., 'Std_ID_058']
     *
     * ...
     */
    public Map<String, Course> loadCourseRegistrations(Path path) throws IOException {
        Map<String, Course> courseMap = new HashMap<>();

        // Satır satır daha rahat kontrol için BufferedReader kullanıyorum
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // boş satırları atla
                }

                // Örnek: "CourseCode_01"
                if (line.startsWith("CourseCode_")) {
                    String courseCode = line;

                    // Şimdi bu course'a ait öğrenci listesi satırını bulalım
                    String listLine = null;
                    while ((listLine = br.readLine()) != null) {
                        listLine = listLine.trim();
                        if (!listLine.isEmpty()) {
                            // bu satır liste satırı
                            break;
                        }
                    }

                    if (listLine == null) {
                        // Dosya bitti, çık
                        break;
                    }

                    // Örnek:
                    // ['Std_ID_170', 'Std_ID_077', ..., 'Std_ID_168']
                    String cleaned = listLine
                            .replace("[", "")
                            .replace("]", "")
                            .replace("'", "")
                            .replace("\"", "");

                    String[] tokens = cleaned.split(",");
                    Course course = courseMap.computeIfAbsent(courseCode, Course::new);

                    for (String token : tokens) {
                        String id = token.trim();
                        if (id.isEmpty()) continue;
                        // Artık id = "Std_ID_170" gibi
                        course.addStudent(id);
                    }
                }
            }
        }

        return courseMap;
    }
}
