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

    // INNER CLASS FOR GETTING USER DETAILS
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

    // ADD USER
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
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkPst = con.prepareStatement(checkSql);
            checkPst.setString(1, username);

            ResultSet rs = checkPst.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(null,
                        "Username already exists. Please use another username.",
                        "Duplicate Username",
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

    // UPDATE USER
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

            String sql;
            PreparedStatement pst;

            // do not change password if blank
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
                // hash new password here
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

    
    

    // DELETE USER
    public boolean deleteUser(int id) {
        try {
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

    // GET USER BY ID
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

    // LOAD USERS TO TABLE
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

    // FILTER USERS
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
}