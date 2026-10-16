# Hospital & Clinic Patient Management System — Phase 1

College-level project using Java 21, Swing, Oracle JDBC and `ojdbc17.jar`. No Maven.

## Files
- `Main.java`: starts the application.
- `DatabaseConnection.java`: creates Oracle JDBC connections.
- `DatabaseConnectionTest.java`: tests connectivity.
- `Patient.java`: patient data class.
- `PatientManager.java`: patient SQL operations and validation.
- `MainFrame.java`: Swing UI and event handling.
- `database/schema.sql`: creates the patient table.

## 1. Place the JDBC driver
Put your existing Oracle JDBC driver at:
`lib/ojdbc17.jar`

## 2. Oracle database setup
First confirm the actual PDB/service name in SQL Developer while connected as SYSTEM:
```sql
SELECT SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name FROM dual;
```
Use the PDB service that your Oracle installation actually provides. Do not assume `XEPDB1` if your result differs.

If using `XEPDB1`, connect as SYSTEM to that service and create a dedicated application user (or reset the password if the user already exists):
```sql
CREATE USER hcpms_user IDENTIFIED BY "Choose_A_Strong_Password";
GRANT CREATE SESSION, CREATE TABLE TO hcpms_user;
ALTER USER hcpms_user QUOTA 100M ON USERS;
```
If the user already exists, use `ALTER USER hcpms_user IDENTIFIED BY "Choose_A_Strong_Password";` instead of `CREATE USER`.

Connect as `HCPMS_USER` and run `database/schema.sql`.

## 3. Configure PowerShell
Run these in the same PowerShell terminal you will use to compile/run. Replace the URL service and password with your actual settings:
```powershell
$env:HCPMS_DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:HCPMS_DB_USER = "hcpms_user"
$env:HCPMS_DB_PASSWORD = "YOUR_PASSWORD"
```
These variables last for the current terminal session.

## 4. Compile all Java files
Run from the project root:
```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -cp "lib\ojdbc17.jar" -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName)
```

## 5. Test database connection
```powershell
java -cp "out;lib\ojdbc17.jar" com.hcpms.DatabaseConnectionTest
```

## 6. Run application
```powershell
java -cp "out;lib\ojdbc17.jar" com.hcpms.Main
```

## Patient module features
Register, search by name or phone, list and update patient details. Phone numbers must be unique.
