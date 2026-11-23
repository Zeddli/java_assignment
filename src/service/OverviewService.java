package service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Payment;
import model.Role;
import model.User;
import repository.PaymentRepository;
import repository.UserRepository;
import util.Validator;

/**
 * Numbers for the Admin Overview dashboard: account counts per role and status,
 * and payment totals per month. The screen only draws what this class returns.
 */
public class OverviewService {
    private final UserRepository userRepo = new UserRepository();
    private final PaymentRepository paymentRepo = new PaymentRepository();

    /** Active and inactive account counts for one role. */
    public static class RoleCount {
        private final int active, inactive;
        RoleCount(int active, int inactive) { this.active = active; this.inactive = inactive; }
        public int getActive() { return active; }
        public int getInactive() { return inactive; }
        public int getTotal() { return active + inactive; }
    }

    /** Number of payments and their total for one calendar month. */
    public static class MonthTotal {
        private final YearMonth month;
        private int count;
        private double total;
        MonthTotal(YearMonth month) { this.month = month; }
        public YearMonth getMonth() { return month; }
        public int getCount() { return count; }
        public double getTotal() { return total; }
        public LocalDate firstDay() { return month.atDay(1); }
        public LocalDate lastDay() { return month.atEndOfMonth(); }
    }

    /** Counts every account of the given role. */
    public RoleCount countRole(Role role) {
        int active = 0, inactive = 0;
        for (User u : userRepo.findAll()) {
            if (u.getRole() != role) continue;
            if (u.isActive()) active++; else inactive++;
        }
        return new RoleCount(active, inactive);
    }

    /** Inactive staff accounts (instructors, facilities and attendance officers). */
    public int countInactiveStaff() {
        int n = 0;
        for (Role r : new Role[]{Role.INSTRUCTOR, Role.FACILITIES, Role.ATTENDANCE}) {
            n += countRole(r).getInactive();
        }
        return n;
    }

    /**
     * Totals for the last {@code months} calendar months, oldest first, ending with the month of
     * {@code today}. Months with no payments are included with zero, so the chart has no gaps.
     */
    public List<MonthTotal> monthlyTotals(int months, LocalDate today) {
        Map<YearMonth, MonthTotal> byMonth = new LinkedHashMap<>();
        YearMonth end = YearMonth.from(today);
        for (int i = months - 1; i >= 0; i--) {
            YearMonth m = end.minusMonths(i);
            byMonth.put(m, new MonthTotal(m));
        }
        for (Payment p : paymentRepo.findAll()) {
            if (!Validator.isDate(p.getPaidDate())) continue;
            MonthTotal t = byMonth.get(YearMonth.from(LocalDate.parse(p.getPaidDate())));
            if (t == null) continue; // outside the window
            t.count++;
            t.total += p.getAmount();
        }
        return new ArrayList<>(byMonth.values());
    }

    /** This calendar month's totals (the last bar of the chart). */
    public MonthTotal thisMonth(LocalDate today) {
        List<MonthTotal> one = monthlyTotals(1, today);
        return one.get(0);
    }
}
