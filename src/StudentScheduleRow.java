public class StudentScheduleRow {

    private String course;
    private int day;
    private int slot;
    private String time;
    private String rooms;

    public StudentScheduleRow(String course, int day, int slot, String time, String rooms) {
        this.course = course;
        this.day = day;
        this.slot = slot;
        this.time = time;
        this.rooms = rooms;
    }

    public String getCourse() { return course; }
    public int getDay() { return day; }
    public int getSlot() { return slot; }
    public String getTime() { return time; }
    public String getRooms() { return rooms; }
}
