package repository;

import java.util.ArrayList;
import java.util.List;
import model.Booking;

public class BookingRepository extends BaseRepository<Booking> {
    public BookingRepository() { super("bookings.txt"); }

    @Override
    protected Booking fromFields(String[] f) { return Booking.fromFields(f); }

    /**
     * Students who may attend a class: booking is BOOKED (not cancelled)
     * and the payment status is PAID. Unpaid bookings are left out.
     */
    public List<Booking> findBookedByClass(String classId) {
        List<Booking> out = new ArrayList<>();
        for (Booking b : findAll()) {
            if (b.getClassId().equals(classId) && b.canAttend()) out.add(b);
        }
        return out;
    }
}