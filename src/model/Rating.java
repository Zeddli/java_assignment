
package model;

/** One line of ratings.txt. */
public class Rating implements Storable {

    private final String ratingId;
    private final String bookingId;
    private final String studentId;
    private final String instructorId;
    private final int score;
    private final String comment;
    private final String date;

    public Rating(String ratingId, String bookingId,
                  String studentId, String instructorId,
                  int score, String comment, String date) {

        this.ratingId = ratingId;
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.instructorId = instructorId;
        this.score = score;
        this.comment = comment;
        this.date = date;
    }

    public static Rating fromFields(String[] f) {
        return new Rating(
                f[0], f[1], f[2], f[3],
                Integer.parseInt(f[4]), f[5], f[6]
        );
    }

    @Override
    public String getId() {
        return ratingId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public int getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toLine() {
        return String.join("|",
                ratingId,
                bookingId,
                studentId,
                instructorId,
                String.valueOf(score),
                comment,
                date
        );
    }
}
