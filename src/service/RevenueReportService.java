
package service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Collections;

import model.Booking;
import model.Payment;
import repository.BookingRepository;
import repository.PaymentRepository;

/**
 * MG3 - Revenue Report.
 * Read-only monthly and yearly revenue calculations.
 */
public class RevenueReportService {

    private final PaymentRepository paymentRepo =
            new PaymentRepository();

    private final BookingRepository bookingRepo =
            new BookingRepository();

    // One row in the revenue report.
    public static class RevenueRow {

        private final String period;
        private final int registrations;
        private final double revenue;

        public RevenueRow(String period,
                          int registrations,
                          double revenue) {
            this.period = period;
            this.registrations = registrations;
            this.revenue = revenue;
        }

        public String getPeriod() {
            return period;
        }

        public int getRegistrations() {
            return registrations;
        }

        public double getRevenue() {
            return revenue;
        }
    }

    // Temporary totals for each reporting period.
    private static class Totals {

        double revenue = 0;
        Set<String> bookingIds = new HashSet<>();
    }

    /**
     * Return available years from PAID transactions.
     */
    public List<Integer> availableYears() {

        Set<Integer> years =
                new TreeSet<>(Collections.reverseOrder());

        for (Payment payment : paymentRepo.findAll()) {

            if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
                continue;
            }

            try {
                LocalDate date =
                        LocalDate.parse(payment.getPaidDate());

                years.add(date.getYear());

            } catch (DateTimeParseException ex) {
                // Ignore invalid dates.
            }
        }

        return new ArrayList<>(years);
    }

    /**
     * Generate the revenue report.
     *
     * monthly: true = monthly, false = yearly
     * year: null = all years
     * month: null = all months
     */
    public List<RevenueRow> generateReport(
            boolean monthly,
            Integer year,
            Integer month) {

        // Find bookings by booking ID.
        Map<String, Booking> bookings = new HashMap<>();

        for (Booking booking : bookingRepo.findAll()) {
            bookings.put(booking.getId(), booking);
        }

        // Sorted by reporting period.
        Map<String, Totals> grouped = new TreeMap<>();

        for (Payment payment : paymentRepo.findAll()) {

            // Only completed payments contribute to revenue.
            if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
                continue;
            }

            LocalDate date;

            try {
                date = LocalDate.parse(payment.getPaidDate());
            } catch (DateTimeParseException ex) {
                continue;
            }

            // Year filter.
            if (year != null && date.getYear() != year) {
                continue;
            }

            // Month filter applies only to monthly reports.
            if (monthly && month != null
                    && date.getMonthValue() != month) {
                continue;
            }

            // Reporting period.
            String period = monthly
                    ? String.format("%04d-%02d",
                            date.getYear(),
                            date.getMonthValue())
                    : String.valueOf(date.getYear());

            Totals totals = grouped.computeIfAbsent(
                    period, key -> new Totals());

            // Sum PAID transaction amounts.
            totals.revenue += payment.getAmount();

            // Count distinct paid bookings.
            Booking booking =
                    bookings.get(payment.getBookingId());

            if (booking != null && booking.isPaid()) {
                totals.bookingIds.add(booking.getId());
            }
        }

        List<RevenueRow> rows = new ArrayList<>();

        for (Map.Entry<String, Totals> entry
                : grouped.entrySet()) {

            Totals totals = entry.getValue();

            rows.add(new RevenueRow(
                    entry.getKey(),
                    totals.bookingIds.size(),
                    totals.revenue));
        }

        return rows;
    }
}
