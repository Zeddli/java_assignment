
package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.Role;
import model.User;
import repository.UserRepository;

/**
 * MG2 - Staff Report service.
 *
 * Provides read-only staff searching, filtering
 * and role totals using the shared UserRepository.
 */
public class StaffReportService {

    private final UserRepository userRepository =
            new UserRepository();

    /**
     * Find staff matching the selected name and role.
     *
     * A null role means all three reportable roles.
     * Both filters are applied together.
     */
    public List<User> search(String name, Role role) {

        String keyword = name == null
                ? ""
                : name.trim().toLowerCase(Locale.ROOT);

        List<User> results = new ArrayList<>();

        for (User user : userRepository.findAll()) {

            Role userRole = user.getRole();

            // Only include the three required staff roles.
            if (userRole != Role.ADMIN
                    && userRole != Role.INSTRUCTOR
                    && userRole != Role.FACILITIES) {
                continue;
            }

            // Apply role filter when selected.
            if (role != null && userRole != role) {
                continue;
            }

            // Apply case-insensitive name search.
            if (!user.getFullName()
                    .toLowerCase(Locale.ROOT)
                    .contains(keyword)) {
                continue;
            }

            results.add(user);
        }

        return results;
    }

    /**
     * Count users belonging to the specified role
     * within the currently filtered results.
     */
    public int countByRole(List<User> staff, Role role) {

        int count = 0;

        for (User user : staff) {
            if (user.getRole() == role) {
                count++;
            }
        }

        return count;
    }
}
