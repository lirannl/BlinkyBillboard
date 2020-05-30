package BillboardSupport;

import java.sql.Timestamp;

public class Schedule {
    public Timestamp StartTime;
    public int duration; // Minutes
    public int repeatInterval;
    public String billboardName;

    public Schedule(Timestamp startTime, int duration, int repeatInterval, String billboardName) {
        StartTime = startTime;
        this.duration = duration;
        this.repeatInterval = repeatInterval;
        this.billboardName = billboardName;
    }
}
