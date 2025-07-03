# MySQL User Setup Instructions for GAWE Application

This document provides instructions to create a MySQL user with proper permissions for the GAWE application to connect to the MySQL database successfully.

## Problem

The GAWE application currently fails to connect to the MySQL database with the error:

```
Access denied for user 'root'@'localhost' (using password: YES)
```

This indicates that the MySQL user credentials configured in the application do not match any valid MySQL user or the user lacks necessary permissions.

## Solution

Create a dedicated MySQL user with a secure password and grant it the necessary permissions on the GAWE database.

### Steps

1. Log in to MySQL as root or an admin user:

```bash
mysql -u root -p
```

2. Create the GAWE database if it does not exist:

```sql
CREATE DATABASE IF NOT EXISTS gawe_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. Create a new MySQL user (replace `gawe_user` and `your_password` with your desired username and a strong password):

```sql
CREATE USER 'gawe_user'@'localhost' IDENTIFIED BY 'your_password';
```

4. Grant all necessary privileges on the `gawe_db` database to the new user:

```sql
GRANT ALL PRIVILEGES ON gawe_db.* TO 'gawe_user'@'localhost';
```

5. Flush privileges to apply changes:

```sql
FLUSH PRIVILEGES;
```

6. Exit MySQL:

```sql
EXIT;
```

## Update Application Configuration

Update the database credentials in the following files to use the new user and password:

- `gawe/src/main/java/id/ac/stis/pbo/demo1/database/DatabaseConfig.java`
- `gawe/src/main/java/id/ac/stis/pbo/demo1/database/MySQLDatabaseManager.java`

Change the following constants:

```java
public static final String DB_USER = "gawe_user";
public static final String DB_PASSWORD = "your_password";
```

Replace `gawe_user` and `your_password` with the actual username and password you created.

## Additional Recommendations

- Avoid using the root user for application database connections for security reasons.
- Use environment variables or configuration files to manage sensitive credentials instead of hardcoding them.
- Restart the GAWE application after updating the credentials.

---

If you want, I can help you update the code files with the new credentials once you provide the username and password you want to use.
