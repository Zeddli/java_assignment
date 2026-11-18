
package repository;

import model.Rating;

public class RatingRepository extends BaseRepository<Rating> {

    public RatingRepository() {
        super("ratings.txt");
    }

    @Override
    protected Rating fromFields(String[] fields) {
        return Rating.fromFields(fields);
    }

    public boolean hasRatedBooking(String bookingId) {

        for (Rating rating : findAll()) {
            if (rating.getBookingId().equals(bookingId)) {
                return true;
            }
        }

        return false;
    }
}
