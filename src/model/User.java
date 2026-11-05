package model;

/** Abstract parent of the six roles (inheritance + abstraction + encapsulation). */
public abstract class User implements Storable {
    private final String userId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String extra;   // instructor: discipline, student: date of birth
    private String status;  // ACTIVE or INACTIVE
 
    protected User(String userId, String username, String password, String fullName,
                   String email, String phone, String extra, String status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.extra = extra;
        this.status = status;
    }
 
    /** Each subclass says which role it is (polymorphism). */
    public abstract String getRole();
 
    @Override public String getId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getExtra() { return extra; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
 
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setStatus(String status) { this.status = status; }
 
    @Override
    public String toLine() {
        return String.join("|", userId, getRole(), username, password, fullName, email, phone, extra, status);
    }
}

