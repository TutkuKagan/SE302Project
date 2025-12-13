public class Classroom {
    private String roomId;
    private int capacity;

    public Classroom(String roomId, int capacity) {
        this.roomId = roomId;
        this.capacity = capacity;
    }

    public String getRoomId() { return roomId; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) {this.capacity = capacity;}
}
