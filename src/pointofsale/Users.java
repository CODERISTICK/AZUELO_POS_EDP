package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Users {

    Connection con;

    public Users() {
        con = DBConnection.getConnection();
    }

    public static class UserDetails {
        public int userId;
        public String firstName;
        public String middleName;
        public String lastName;
        public String username;
        public String role;
        public String status;
        public String createdAt;
        public String updatedAt;
    }

    public boolean addUser(String firstName, String middleName, String lastName,
            String username, String password, String role, String status) {

        firstName = firstName.trim();
        middleName = middleName.trim();
        lastName = lastName.trim();
        username = username.trim();
        password = password.trim();

        if (firstName.isEmpty() || lastName.isEmpty()
                || username.isEmpty() || password.isEmpty()) {

            JOptionPane.showMessageDialog(null,
                    "First name, last name, username, and password are required.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            String checkNameSql = "SELECT COUNT(*) FROM users "
                    + "WHERE first_name = ? AND middle_name = ? AND last_name = ?";
            PreparedStatement checkNamePst = con.prepareStatement(checkNameSql);
            checkNamePst.setString(1, firstName);
            checkNamePst.setString(2, middleName);
            checkNamePst.setString(3, lastName);

            ResultSet rsName = checkNamePst.executeQuery();
            if (rsName.next() && rsName.getInt(1) > 0) {
                JOptionPane.showMessageDialog(null,
                        "User already exists with the same full name.",
                        "Duplicate User",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            String checkUsernameSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkUserPst = con.prepareStatement(checkUsernameSql);
            checkUserPst.setString(1, username);

            ResultSet rsUser = checkUserPst.executeQuery();
            if (rsUser.next() && rsUser.getInt(1) > 0) {
                JOptionPane.showMessageDialog(null,
                        "Username already exists. Please use another username.",
                        "Duplicate Username",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            if ("Super Admin".equals(role) && isSuperAdminExists()) {
                JOptionPane.showMessageDialog(null,
                        "Only one Super Admin account is allowed in the system.",
                        "Super Admin Restricted",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            String hashedPassword = PasswordUtils.hashPassword(password);

            String sql = "INSERT INTO users "
                    + "(first_name, middle_name, last_name, username, password, role, status, created_at, updated_at) "
                    + "VALUES (?,?,?,?,?,?,?,NOW(),NOW())";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, firstName);
            pst.setString(2, middleName);
            pst.setString(3, lastName);
            pst.setString(4, username);
            pst.setString(5, hashedPassword);
            pst.setString(6, role);
            pst.setString(7, status);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null,
                    "User successfully added!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error adding user: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(int id, String firstName, String middleName,
            String lastName, String username, String password,
            String role, String status) {

        firstName = firstName.trim();
        middleName = middleName.trim();
        lastName = lastName.trim();
        username = username.trim();
        password = password.trim();

        if (id <= 0) {
            JOptionPane.showMessageDialog(null, "Invalid user selected.");
            return false;
        }

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "First name, last name, and username are required.");
            return false;
        }

        try {
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ? AND user_id <> ?";
            PreparedStatement checkPst = con.prepareStatement(checkSql);
            checkPst.setString(1, username);
            checkPst.setInt(2, id);

            ResultSet rs = checkPst.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(null, "Username already exists.");
                return false;
            }

            if (isFullNameExists(firstName, middleName, lastName, id)) {
                JOptionPane.showMessageDialog(null,
                        "User already exists with the same full name.",
                        "Duplicate User",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            UserDetails currentUser = getUserById(id);
            if ("Super Admin".equals(role)
                    && currentUser != null
                    && !"Super Admin".equals(currentUser.role)
                    && isSuperAdminExists()) {
                JOptionPane.showMessageDialog(null,
                        "Only one Super Admin account is allowed in the system.",
                        "Super Admin Restricted",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            String sql;
            PreparedStatement pst;

            if (password.isEmpty()) {
                sql = "UPDATE users SET first_name=?, middle_name=?, last_name=?, "
                        + "username=?, role=?, status=?, updated_at=NOW() "
                        + "WHERE user_id=?";

                pst = con.prepareStatement(sql);
                pst.setString(1, firstName);
                pst.setString(2, middleName);
                pst.setString(3, lastName);
                pst.setString(4, username);
                pst.setString(5, role);
                pst.setString(6, status);
                pst.setInt(7, id);

            } else {
                String hashedPassword = PasswordUtils.hashPassword(password);

                sql = "UPDATE users SET first_name=?, middle_name=?, last_name=?, "
                        + "username=?, password=?, role=?, status=?, updated_at=NOW() "
                        + "WHERE user_id=?";

                pst = con.prepareStatement(sql);
                pst.setString(1, firstName);
                pst.setString(2, middleName);
                pst.setString(3, lastName);
                pst.setString(4, username);
                pst.setString(5, hashedPassword);
                pst.setString(6, role);
                pst.setString(7, status);
                pst.setInt(8, id);
            }

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "User updated successfully!");
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int id) {
        try {
            UserDetails user = getUserById(id);
            if (user != null && "Super Admin".equals(user.role)) {
                JOptionPane.showMessageDialog(null,
                        "Super Admin account cannot be deleted.",
                        "Delete Blocked",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            String sql = "DELETE FROM users WHERE user_id=?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, id);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "User deleted successfully!");
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error deleting user: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    public UserDetails getUserById(int userId) {
        UserDetails user = null;

        try {
            String sql = "SELECT user_id, first_name, middle_name, last_name, username, role, status, created_at, updated_at "
                    + "FROM users WHERE user_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, userId);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                user = new UserDetails();
                user.userId = rs.getInt("user_id");
                user.firstName = rs.getString("first_name");
                user.middleName = rs.getString("middle_name");
                user.lastName = rs.getString("last_name");
                user.username = rs.getString("username");
                user.role = rs.getString("role");
                user.status = rs.getString("status");
                user.createdAt = rs.getString("created_at");
                user.updatedAt = rs.getString("updated_at");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error loading user details: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        return user;
    }

    public void loadUsers(JTable table) {
        try {
            String sql = "SELECT user_id, first_name, middle_name, last_name, role, status, created_at, updated_at "
                    + "FROM users ORDER BY user_id ASC";

            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                int id = rs.getInt("user_id");

                String firstName = rs.getString("first_name") == null ? "" : rs.getString("first_name");
                String middleName = rs.getString("middle_name") == null ? "" : rs.getString("middle_name");
                String lastName = rs.getString("last_name") == null ? "" : rs.getString("last_name");

                String fullname = (firstName + " " + middleName + " " + lastName).replaceAll("\\s+", " ").trim();

                String role = rs.getString("role");
                String status = rs.getString("status");
                String created = rs.getString("created_at");
                String updated = rs.getString("updated_at");

                model.addRow(new Object[]{
                    id,
                    fullname,
                    role,
                    status,
                    created,
                    updated
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void filterUsers(JTable table, String search, String roleFilter) {
        try {
            String sql = "SELECT user_id, first_name, middle_name, last_name, role, status, created_at, updated_at "
                    + "FROM users WHERE 1=1 ";

            if (!roleFilter.equals("ALL")) {
                sql += "AND role = ? ";
            }

            sql += "AND CONCAT(COALESCE(first_name,''), ' ', COALESCE(middle_name,''), ' ', COALESCE(last_name,''), ' ', COALESCE(username,'')) LIKE ? ";
            sql += "ORDER BY user_id ASC";

            PreparedStatement pst = con.prepareStatement(sql);

            int paramIndex = 1;

            if (!roleFilter.equals("ALL")) {
                pst.setString(paramIndex++, roleFilter);
            }

            pst.setString(paramIndex, "%" + search + "%");

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                int id = rs.getInt("user_id");

                String firstName = rs.getString("first_name") == null ? "" : rs.getString("first_name");
                String middleName = rs.getString("middle_name") == null ? "" : rs.getString("middle_name");
                String lastName = rs.getString("last_name") == null ? "" : rs.getString("last_name");

                String fullname = (firstName + " " + middleName + " " + lastName).replaceAll("\\s+", " ").trim();

                String role = rs.getString("role");
                String status = rs.getString("status");
                String created = rs.getString("created_at");
                String updated = rs.getString("updated_at");

                model.addRow(new Object[]{
                    id,
                    fullname,
                    role,
                    status,
                    created,
                    updated
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isUsernameExists(String username, int excludeUserId) {
        try {
            String sql;
            PreparedStatement pst;

            if (excludeUserId > 0) {
                sql = "SELECT COUNT(*) FROM users WHERE username = ? AND user_id <> ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, username);
                pst.setInt(2, excludeUserId);
            } else {
                sql = "SELECT COUNT(*) FROM users WHERE username = ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, username);
            }

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isNameExists(String firstName, String middleName, String lastName, int excludeUserId) {
        try {
            String normFirst = firstName == null ? "" : firstName.trim().replaceAll("\\s+", " ").toLowerCase();
            String normMiddle = middleName == null ? "" : middleName.trim().replaceAll("\\s+", " ").toLowerCase();
            String normLast = lastName == null ? "" : lastName.trim().replaceAll("\\s+", " ").toLowerCase();

            String sql;
            PreparedStatement pst;

            if (excludeUserId > 0) {
                sql = "SELECT COUNT(*) FROM users WHERE " +
                      "LOWER(TRIM(REPLACE(first_name, '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(COALESCE(middle_name, ''), '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(last_name, '  ', ' '))) = ? AND " +
                      "user_id <> ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, normFirst);
                pst.setString(2, normMiddle);
                pst.setString(3, normLast);
                pst.setInt(4, excludeUserId);
            } else {
                sql = "SELECT COUNT(*) FROM users WHERE " +
                      "LOWER(TRIM(REPLACE(first_name, '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(COALESCE(middle_name, ''), '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(last_name, '  ', ' '))) = ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, normFirst);
                pst.setString(2, normMiddle);
                pst.setString(3, normLast);
            }

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if First + Last name exists (for soft warning) - regardless of middle name
    public boolean isFirstLastNameExists(String firstName, String lastName, int excludeUserId) {
        try {
            String normFirst = firstName == null ? "" : firstName.trim().replaceAll("\\s+", " ").toLowerCase();
            String normLast = lastName == null ? "" : lastName.trim().replaceAll("\\s+", " ").toLowerCase();

            String sql;
            PreparedStatement pst;

            if (excludeUserId > 0) {
                sql = "SELECT COUNT(*) FROM users WHERE " +
                      "LOWER(TRIM(REPLACE(first_name, '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(last_name, '  ', ' '))) = ? AND " +
                      "user_id <> ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, normFirst);
                pst.setString(2, normLast);
                pst.setInt(3, excludeUserId);
            } else {
                sql = "SELECT COUNT(*) FROM users WHERE " +
                      "LOWER(TRIM(REPLACE(first_name, '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(last_name, '  ', ' '))) = ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, normFirst);
                pst.setString(2, normLast);
            }

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if full name (First + Middle + Last) exists (for hard block)
    public boolean isFullNameExists(String firstName, String middleName, String lastName, int excludeUserId) {
        try {
            String normFirst = firstName == null ? "" : firstName.trim().replaceAll("\\s+", " ").toLowerCase();
            String normMiddle = middleName == null ? "" : middleName.trim().replaceAll("\\s+", " ").toLowerCase();
            String normLast = lastName == null ? "" : lastName.trim().replaceAll("\\s+", " ").toLowerCase();

            String sql;
            PreparedStatement pst;

            if (excludeUserId > 0) {
                sql = "SELECT COUNT(*) FROM users WHERE " +
                      "LOWER(TRIM(REPLACE(first_name, '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(COALESCE(middle_name, ''), '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(last_name, '  ', ' '))) = ? AND " +
                      "user_id <> ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, normFirst);
                pst.setString(2, normMiddle);
                pst.setString(3, normLast);
                pst.setInt(4, excludeUserId);
            } else {
                sql = "SELECT COUNT(*) FROM users WHERE " +
                      "LOWER(TRIM(REPLACE(first_name, '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(COALESCE(middle_name, ''), '  ', ' '))) = ? AND " +
                      "LOWER(TRIM(REPLACE(last_name, '  ', ' '))) = ?";
                pst = con.prepareStatement(sql);
                pst.setString(1, normFirst);
                pst.setString(2, normMiddle);
                pst.setString(3, normLast);
            }

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if Super Admin already exists (for validation)
    public boolean isSuperAdminExists() {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE role = 'Super Admin'";
            PreparedStatement pst = con.prepareStatement(sql);
            
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}