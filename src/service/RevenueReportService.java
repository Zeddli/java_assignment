package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import model.Booking;
import model.ClassSession;
import model.Payment;
import repository.BookingRepository;
import repository.ClassRepository;
import repository.PaymentRepository;

/**
 * MG3: Read-only reporting based on PAID payment transactions.
 * Payment dates determine the reporting period; bookings link payments to classes.
 */
public class RevenueReportService {

    private final PaymentRepository payments = new PaymentRepository();
    private final BookingRepository bookings = new BookingRepository();
    private final ClassRepository classes = new ClassRepository();

    /** One period/class row shown in the main revenue report table. */
    public static final class ClassRevenueRow {
        private final String period;
        private final String classId;
        private final String discipline;
        private final int registrations;
        private final BigDecimal revenue;

        private ClassRevenueRow(String period, String classId, String discipline,
                                int registrations, BigDecimal revenue) {
            this.period = period;
            this.classId = classId;
            this.discipline = discipline;
            this.registrations = registrations;
            this.revenue = revenue;
        }

        public String getPeriod() { return period; }
        public String getClassId() { return classId; }
        public String getDiscipline() { return discipline; }
        public int getRegistrations() { return registrations; }
        public BigDecimal getRevenue() { return revenue; }
    }

    /** Amount and distinct paid registrations grouped by a particular category. */
    public static final class BreakdownRow {
        private final String category;
        private final int registrations;
        private final BigDecimal revenue;

        private BreakdownRow(String category, int registrations, BigDecimal revenue) {
            this.category = category;
            this.registrations = registrations;
            this.revenue = revenue;
        }

        public String getCategory() { return category; }
        public int getRegistrations() { return registrations; }
        public BigDecimal getRevenue() { return revenue; }
    }

    /** One month shown by the Swing bar chart. */
    public static final class MonthRevenue {
        private final YearMonth month;
        private final BigDecimal revenue;

        private MonthRevenue(YearMonth month, BigDecimal revenue) {
            this.month = month;
            this.revenue = revenue;
        }

        public YearMonth getMonth() { return month; }
        public String getLabel() { return month.toString(); }
        public BigDecimal getRevenue() { return revenue; }
    }

    /** All results for one application of the current filters. */
    public static final class Report {
        private final List<ClassRevenueRow> classes;
        private final List<BreakdownRow> methods;
        private final List<BreakdownRow> disciplines;
        private final List<MonthRevenue> monthlyRevenue;
        private final int registrations;
        private final BigDecimal revenue;
        private final Double growthPercent;
        private final String comparison;

        private Report(List<ClassRevenueRow> classes, List<BreakdownRow> methods,
                       List<BreakdownRow> disciplines, List<MonthRevenue> monthlyRevenue,
                       int registrations, BigDecimal revenue,
                       Double growthPercent, String comparison) {
            this.classes = classes;
            this.methods = methods;
            this.disciplines = disciplines;
            this.monthlyRevenue = monthlyRevenue;
            this.registrations = registrations;
            this.revenue = revenue;
            this.growthPercent = growthPercent;
            this.comparison = comparison;
        }

        public List<ClassRevenueRow> getClasses() { return classes; }
        public List<BreakdownRow> getMethods() { return methods; }
        public List<BreakdownRow> getDisciplines() { return disciplines; }
        public List<MonthRevenue> getMonthlyRevenue() { return monthlyRevenue; }
        public int getRegistrations() { return registrations; }
        public BigDecimal getRevenue() { return revenue; }
        /** Null if a comparable previous period is unavailable. */
        public Double getGrowthPercent() { return growthPercent; }
        public String getComparison() { return comparison; }
    }

    private static final class PaymentRecord {
        final LocalDate date;
        final String bookingId;
        final String classId;
        final String discipline;
        final String method;
        final boolean paidBooking;
        final BigDecimal amount;

        PaymentRecord(LocalDate date, Payment payment, Booking booking, ClassSession session) {
            this.date = date;
            bookingId = payment.getBookingId();
            classId = booking == null ? "(Unknown)" : booking.getClassId();
            discipline = session == null ? "(Unknown)" : session.getDiscipline();
            method = payment.getMethod().isBlank() ? "(Unknown)" : payment.getMethod();
            paidBooking = booking != null && booking.isPaid();
            amount = BigDecimal.valueOf(payment.getAmount());
        }
    }

    private static final class Totals {
        BigDecimal amount = BigDecimal.ZERO;
        final Set<String> registrations = new HashSet<>();

        void add(PaymentRecord record) {
            amount = amount.add(record.amount);
            if (record.paidBooking) registrations.add(record.bookingId);
        }
    }

    /** Returns all years containing PAID transactions with valid payment dates, newest first. */
    public List<Integer> availableYears() {
        Set<Integer> years = new TreeSet<>(Collections.reverseOrder());
        for (PaymentRecord record : loadPaid()) years.add(record.date.getYear());
        return new ArrayList<>(years);
    }

    /**
     * @param monthly true: group main table by yyyy-MM/class; false: yyyy/class
     * @param year null for all years
     * @param month 1-12 or null; ignored in yearly mode
     * @param from inclusive, or null
     * @param to inclusive, or null
     */
    public Report generateReport(boolean monthly, Integer year, Integer month,
                                 LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after To date.");
        }

        // Keep a copy of every paid transaction for the previous-period comparison.
        List<PaymentRecord> allPaid = loadPaid();
        List<PaymentRecord> visible = new ArrayList<>();
        Map<String, Totals> byClass = new TreeMap<>();
        Map<String, Totals> byMethod = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Totals> byDiscipline = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        Totals overall = new Totals();

        for (PaymentRecord record : allPaid) {
            if (year != null && record.date.getYear() != year) continue;
            if (monthly && month != null && record.date.getMonthValue() != month) continue;
            if (from != null && record.date.isBefore(from)) continue;
            if (to != null && record.date.isAfter(to)) continue;

            visible.add(record);
            overall.add(record);
            String period = monthly ? YearMonth.from(record.date).toString()
                                    : Integer.toString(record.date.getYear());
            String key = period + "\t" + record.classId + "\t" + record.discipline;
            byClass.computeIfAbsent(key, ignored -> new Totals()).add(record);
            byMethod.computeIfAbsent(record.method, ignored -> new Totals()).add(record);
            byDiscipline.computeIfAbsent(record.discipline, ignored -> new Totals()).add(record);
            YearMonth periodMonth = YearMonth.from(record.date);
            byMonth.merge(periodMonth, record.amount, BigDecimal::add);
        }

        List<ClassRevenueRow> classRows = new ArrayList<>();
        for (Map.Entry<String, Totals> entry : byClass.entrySet()) {
            String[] keys = entry.getKey().split("\t", -1);
            classRows.add(new ClassRevenueRow(keys[0], keys[1], keys[2],
                    entry.getValue().registrations.size(), entry.getValue().amount));
        }

        List<MonthRevenue> chartRows = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> entry : byMonth.entrySet()) {
            chartRows.add(new MonthRevenue(entry.getKey(), entry.getValue()));
        }

        Double growth = null;
        String comparison;
        if (from != null || to != null) {
            // A partial date range must not be compared with a whole previous month/year.
            comparison = "Growth unavailable for custom date ranges";
        } else if (visible.isEmpty()) {
            comparison = "No period to compare";
        } else {
            LocalDate latest = visible.stream().map(r -> r.date).max(LocalDate::compareTo).get();
            int currentYear = latest.getYear();
            YearMonth currentMonth = YearMonth.from(latest);
            YearMonth previousMonth = currentMonth.minusMonths(1);
            int previousYear = currentYear - 1;

            BigDecimal currentTotal = BigDecimal.ZERO;
            BigDecimal previousTotal = BigDecimal.ZERO;
            int previousPayments = 0;
            for (PaymentRecord r : allPaid) {
                boolean current = monthly ? YearMonth.from(r.date).equals(currentMonth)
                                          : r.date.getYear() == currentYear;
                boolean previous = monthly ? YearMonth.from(r.date).equals(previousMonth)
                                           : r.date.getYear() == previousYear;
                if (current) currentTotal = currentTotal.add(r.amount);
                if (previous) {
                    previousTotal = previousTotal.add(r.amount);
                    previousPayments++;
                }
            }

            String currentLabel = monthly ? monthName(currentMonth) : String.valueOf(currentYear);
            String previousLabel = monthly ? monthName(previousMonth) : String.valueOf(previousYear);
            comparison = currentLabel + " vs " + previousLabel;
            if (previousPayments > 0 && previousTotal.signum() > 0) {
                growth = currentTotal.subtract(previousTotal).doubleValue()
                        / previousTotal.doubleValue() * 100.0;
            }
        }

        return new Report(classRows, breakdown(byMethod), breakdown(byDiscipline), chartRows,
                overall.registrations.size(), overall.amount, growth, comparison);
    }

    private static String monthName(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                + " " + month.getYear();
    }

    private static List<BreakdownRow> breakdown(Map<String, Totals> map) {
        List<BreakdownRow> rows = new ArrayList<>();
        for (Map.Entry<String, Totals> entry : map.entrySet()) {
            rows.add(new BreakdownRow(entry.getKey(), entry.getValue().registrations.size(),
                    entry.getValue().amount));
        }
        return rows;
    }

    /** Load only PAID payments, matching each to its booking and class when available. */
    private List<PaymentRecord> loadPaid() {
        Map<String, Booking> bookingMap = new HashMap<>();
        for (Booking booking : bookings.findAll()) bookingMap.put(booking.getId(), booking);

        Map<String, ClassSession> classMap = new HashMap<>();
        for (ClassSession session : classes.findAll()) classMap.put(session.getId(), session);

        List<PaymentRecord> rows = new ArrayList<>();
        for (Payment payment : payments.findAll()) {
            if (!"PAID".equalsIgnoreCase(payment.getStatus())) continue;
            try {
                LocalDate date = LocalDate.parse(payment.getPaidDate());
                Booking booking = bookingMap.get(payment.getBookingId());
                ClassSession session = booking == null ? null : classMap.get(booking.getClassId());
                rows.add(new PaymentRecord(date, payment, booking, session));
            } catch (DateTimeParseException ex) {
                System.err.println("Skipping payment with invalid paid date: " + payment.getId());
            }
        }
        return rows;
    }
}
