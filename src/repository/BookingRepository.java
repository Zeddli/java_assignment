package repository;
 
import java.util.ArrayList;
import java.util.List;
import model.Booking;
 
public class BookingRepository extends BaseRepository<Booking> {
    public BookingRepository() { super("bookings.txt"); }
 
    @Override
    protected Booking fromFields(String[] f) { return Booking.fromFields(f); }
 
    /** Only students whose booking is still BOOKED (not cancelled). */
    public List<Booking> findBookedByClass(String classId) {
        List<Booking> out = new ArrayList<>();
        for (Booking b : findAll()) if (b.getClassId().equals(classId) && b.isBooked()) out.add(b);
        return out;
    }
}

