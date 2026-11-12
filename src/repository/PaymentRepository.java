package repository;

import java.util.ArrayList;
import java.util.List;
import model.Payment;

/** payments.txt. Written by ST2 (Student), read by AD3, MG3 and ST2 history. */
public class PaymentRepository extends BaseRepository<Payment> {
    public PaymentRepository() { super("payments.txt"); }

    @Override
    protected Payment fromFields(String[] f) { return Payment.fromFields(f); }

    public List<Payment> findByStudent(String studentId) {
        List<Payment> out = new ArrayList<>();
        for (Payment p : findAll()) if (p.getStudentId().equals(studentId)) out.add(p);
        return out;
    }
}
