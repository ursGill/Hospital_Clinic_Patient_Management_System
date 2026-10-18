package com.hcpms;

import javax.swing.*;

import javax.swing.table.DefaultTableModel;

import java.awt.*;

import java.sql.SQLException;

import java.util.List;

public class MainFrame extends JFrame {

    private final PatientManager patientManager;

    private final DoctorManager doctorManager;

    private final AppointmentManager appointmentManager;

    private final DatabaseConnection database;

    // Patient form

    private final JTextField pName = new JTextField(15);

    private final JTextField pAge = new JTextField(5);

    private final JComboBox<String> pGender =

            new JComboBox<>(new String[]{

                    "Select...", "Female", "Male", "Other"

            });

    private final JTextField pPhone = new JTextField(12);

    private final JTextField pAddress = new JTextField(18);

    private final JTextField pSearch = new JTextField(15);

    private int selectedPatientId;

    private final DefaultTableModel pModel =

            new DefaultTableModel(

                    new String[]{"ID", "Name", "Age", "Gender",

                            "Phone", "Address", "Created At"}, 0) {

                @Override

                public boolean isCellEditable(int row, int column) {

                    return false;

                }

            };

    private final JTable pTable = new JTable(pModel);

    // Doctor form

    private final JTextField dName = new JTextField(15);

    private final JTextField dSpecialization = new JTextField(15);

    private final JTextField dPhone = new JTextField(12);

    private final JTextField dFee = new JTextField(8);

    private final JTextField dSearch = new JTextField(15);

    private final JLabel dStatus =

            new JLabel("No doctor selected");

    private JButton dDeleteButton;

    private int selectedDoctorId;

    private final DefaultTableModel dModel =

            new DefaultTableModel(

                    new String[]{"ID", "Name", "Specialization",

                            "Phone", "Fee", "Created At"}, 0) {

                @Override

                public boolean isCellEditable(int row, int column) {

                    return false;

                }

            };

    private final JTable dTable = new JTable(dModel);

    public MainFrame(PatientManager patientManager,

                     DoctorManager doctorManager, AppointmentManager appointmentManager,

                    DatabaseConnection database) {

        this.patientManager = patientManager;

        this.doctorManager = doctorManager;

        this.appointmentManager = appointmentManager;

        this.database = database;

        setTitle("Hospital & Clinic Patient Management System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(1100, 680);

        setMinimumSize(new Dimension(900, 540));

        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        tabs.insertTab(

        "Dashboard",

        null,

        new DashboardPanel(database),

        "Hospital overview",

        0

);

        tabs.addTab("Patients", createPatientPanel());

        tabs.addTab("Doctors", createDoctorPanel());

        tabs.addTab("Appointments",

        new AppointmentPanel(

                patientManager,

                doctorManager,

                appointmentManager

        ));

        add(tabs);

        UIStyle.styleTree(this);
        applyModuleStyling();

        loadPatients();

        loadDoctors();

    }

    // ================= PATIENT UI =================

    private JPanel createPatientPanel() {

        JPanel panel = new JPanel(new BorderLayout(8, 8));

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());

        form.setBorder(

                BorderFactory.createTitledBorder("Patient Details"));

        GridBagConstraints c = constraints();

        addField(form, c, 0, 0, "Name:", pName);

        addField(form, c, 2, 0, "Age:", pAge);

        addField(form, c, 0, 1, "Gender:", pGender);

        addField(form, c, 2, 1, "Phone:", pPhone);

        addField(form, c, 0, 2, "Address:", pAddress);

        JButton register = new JButton("Register Patient");

        register.addActionListener(e -> addPatient());

        JButton update = new JButton("Update Selected");

        update.addActionListener(e -> updatePatient());

        JButton clear = new JButton("Clear");

        clear.addActionListener(e -> clearPatientForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        buttons.add(register);

        buttons.add(update);

        buttons.add(clear);

        c.gridx = 0;

        c.gridy = 3;

        c.gridwidth = 4;

        form.add(buttons, c);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));

        bottom.add(new JLabel("Search name/phone:"));

        bottom.add(pSearch);

        JButton search = new JButton("Search");

        search.addActionListener(e -> searchPatients());

        JButton refresh = new JButton("Show All");

        refresh.addActionListener(e -> {
            pSearch.setText("");
            loadPatients();
        });

        bottom.add(search);

        bottom.add(refresh);

        pTable.setAutoCreateRowSorter(true);

        pTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        pTable.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()

                    && pTable.getSelectedRow() >= 0) {

                int row = pTable.convertRowIndexToModel(

                        pTable.getSelectedRow());

                selectedPatientId =

                        Integer.parseInt(pModel.getValueAt(row, 0).toString());

                pName.setText(cell(pModel, row, 1));

                pAge.setText(cell(pModel, row, 2));

                pGender.setSelectedItem(cell(pModel, row, 3));

                pPhone.setText(cell(pModel, row, 4));

                pAddress.setText(cell(pModel, row, 5));

            }

        });

        panel.add(form, BorderLayout.NORTH);

        panel.add(new JScrollPane(pTable), BorderLayout.CENTER);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;

    }

    private void addPatient() {

        try {

            int age = Integer.parseInt(pAge.getText().trim());

            String gender = (String) pGender.getSelectedItem();

            if (gender == null || gender.equals("Select...")) {

                throw new IllegalArgumentException("Select a gender.");

            }

            Patient patient = new Patient(

                    pName.getText(), age, gender,

                    pPhone.getText(), pAddress.getText());

            int id = patientManager.registerPatient(patient);

            JOptionPane.showMessageDialog(

                    this, "Patient registered. ID: " + id);

            clearPatientForm();

            loadPatients();

        } catch (NumberFormatException ex) {

            warn("Age must be a whole number.");

        } catch (IllegalArgumentException ex) {

            warn(ex.getMessage());

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void updatePatient() {

        if (selectedPatientId <= 0) {

            warn("Select a patient first.");

            return;

        }

        try {

            int age = Integer.parseInt(pAge.getText().trim());

            String gender = (String) pGender.getSelectedItem();

            if (gender == null || gender.equals("Select...")) {

                throw new IllegalArgumentException("Select a gender.");

            }

            Patient patient = new Patient(

                    pName.getText(), age, gender,

                    pPhone.getText(), pAddress.getText());

            patient.setPatientId(selectedPatientId);

            if (patientManager.updatePatient(patient)) {

                JOptionPane.showMessageDialog(

                        this, "Patient updated successfully.");

                loadPatients();

            } else {

                warn("Patient not found.");

            }

        } catch (NumberFormatException ex) {

            warn("Age must be a whole number.");

        } catch (IllegalArgumentException ex) {

            warn(ex.getMessage());

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void searchPatients() {

        try {

            displayPatients(

                    patientManager.searchPatients(pSearch.getText()));

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void loadPatients() {

        try {

            displayPatients(patientManager.getAllPatients());

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void displayPatients(List<Patient> patients) {

        pModel.setRowCount(0);

        for (Patient p : patients) {

            pModel.addRow(new Object[]{

                    p.getPatientId(), p.getFullName(), p.getAge(),

                    p.getGender(), p.getPhone(), p.getAddress(),

                    p.getCreatedAt()

            });

        }

    }

    private void clearPatientForm() {

        selectedPatientId = 0;

        pName.setText("");

        pAge.setText("");

        pGender.setSelectedIndex(0);

        pPhone.setText("");

        pAddress.setText("");

        pTable.clearSelection();

    }

    // ================= DOCTOR UI =================

    private JPanel createDoctorPanel() {

        JPanel panel = new JPanel(new BorderLayout(8, 8));

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());

        form.setBorder(

                BorderFactory.createTitledBorder("Doctor Details"));

        GridBagConstraints c = constraints();

        addField(form, c, 0, 0, "Full name:", dName);

        addField(form, c, 2, 0, "Specialization:", dSpecialization);

        addField(form, c, 0, 1, "Phone:", dPhone);

        addField(form, c, 2, 1, "Consultation fee:", dFee);

        JButton add = new JButton("Add Doctor");

        add.addActionListener(e -> addDoctor());

        JButton update = new JButton("Update Selected");

        update.addActionListener(e -> updateDoctor());

        JButton clear = new JButton("Clear");

        clear.addActionListener(e -> clearDoctorForm());

        dDeleteButton = new JButton("Delete Selected");
        dDeleteButton.setToolTipText("Delete the selected doctor if no appointments reference them");
        dDeleteButton.addActionListener(e -> deleteDoctor());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        buttons.add(add);

        buttons.add(update);

        buttons.add(clear);

        buttons.add(dDeleteButton);

        c.gridx = 0;

        c.gridy = 2;

        c.gridwidth = 4;

        form.add(buttons, c);

        JPanel bottom = new JPanel(new BorderLayout());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchPanel.add(new JLabel("Search name/specialization:"));

        searchPanel.add(dSearch);

        JButton search = new JButton("Search");

        search.addActionListener(e -> searchDoctors());

        JButton refresh = new JButton("Show All");

        refresh.addActionListener(e -> {
            dSearch.setText("");
            loadDoctors();
        });

        searchPanel.add(search);

        searchPanel.add(refresh);

        bottom.add(searchPanel, BorderLayout.NORTH);

        bottom.add(dStatus, BorderLayout.SOUTH);

        dTable.setAutoCreateRowSorter(true);

        dTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        dTable.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()

                    && dTable.getSelectedRow() >= 0) {

                int row = dTable.convertRowIndexToModel(

                        dTable.getSelectedRow());

                selectedDoctorId =

                        Integer.parseInt(dModel.getValueAt(row, 0).toString());

                dName.setText(cell(dModel, row, 1));

                dSpecialization.setText(cell(dModel, row, 2));

                dPhone.setText(cell(dModel, row, 3));

                dFee.setText(cell(dModel, row, 4));

                dStatus.setText(

                        "Selected doctor ID: " + selectedDoctorId);

            }

        });

        panel.add(form, BorderLayout.NORTH);

        panel.add(new JScrollPane(dTable), BorderLayout.CENTER);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;

    }

    private void addDoctor() {

        try {

            Doctor doctor = readDoctorForm();

            int id = doctorManager.addDoctor(doctor);

            JOptionPane.showMessageDialog(

                    this, "Doctor registered. ID: " + id);

            clearDoctorForm();

            loadDoctors();

        } catch (NumberFormatException ex) {

            warn("Consultation fee must be a number, e.g. 500 or 500.50.");

        } catch (IllegalArgumentException ex) {

            warn(ex.getMessage());

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void updateDoctor() {

        if (selectedDoctorId <= 0) {

            warn("Select a doctor from the table first.");

            return;

        }

        try {

            Doctor doctor = readDoctorForm();

            doctor.setDoctorId(selectedDoctorId);

            if (doctorManager.updateDoctor(doctor)) {

                JOptionPane.showMessageDialog(

                        this, "Doctor updated successfully.");

                loadDoctors();

            } else {

                warn("Doctor not found.");

            }

        } catch (NumberFormatException ex) {

            warn("Consultation fee must be a number, e.g. 500 or 500.50.");

        } catch (IllegalArgumentException ex) {

            warn(ex.getMessage());

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void deleteDoctor() {

        if (selectedDoctorId <= 0) {
            warn("Select a doctor from the table before deleting.");
            return;
        }

        String doctorName = dName.getText().trim();
        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete doctor \"" + doctorName
                        + "\" (ID: " + selectedDoctorId + ")?\n"
                        + "This action cannot be undone.",
                "Confirm Doctor Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        int doctorIdToDelete = selectedDoctorId;
        try {
            boolean deleted = doctorManager.deleteDoctor(doctorIdToDelete);

            if (deleted) {
                JOptionPane.showMessageDialog(
                        this,
                        "Doctor deleted successfully.",
                        "Doctor Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
                clearDoctorForm();
                dSearch.setText("");
                loadDoctors();
            } else {
                warn("Doctor not found. Refresh the list and try again.");
            }
        } catch (SQLException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("ORA-02292")) {
                warn("This doctor cannot be deleted because appointments are linked to them. "
                        + "Cancel or remove the related appointments first.");
            } else {
                databaseError(ex);
            }
        }
    }

    private Doctor readDoctorForm() {

        if (dFee.getText().isBlank()) {

            throw new IllegalArgumentException(

                    "Consultation fee is required.");

        }

        double fee = Double.parseDouble(dFee.getText().trim());

        return new Doctor(

                dName.getText(),

                dSpecialization.getText(),

                dPhone.getText(),

                fee

        );

    }

    private void searchDoctors() {

        try {

            displayDoctors(

                    doctorManager.searchDoctors(dSearch.getText()));

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void loadDoctors() {

        try {

            displayDoctors(doctorManager.getAllDoctors());

        } catch (SQLException ex) {

            databaseError(ex);

        }

    }

    private void displayDoctors(List<Doctor> doctors) {

        dModel.setRowCount(0);

        for (Doctor d : doctors) {

            dModel.addRow(new Object[]{

                    d.getDoctorId(),

                    d.getFullName(),

                    d.getSpecialization(),

                    d.getPhone(),

                    String.format("%.2f", d.getConsultationFee()),

                    d.getCreatedAt()

            });

        }

    }

    private void clearDoctorForm() {

        selectedDoctorId = 0;

        dName.setText("");

        dSpecialization.setText("");

        dPhone.setText("");

        dFee.setText("");

        dTable.clearSelection();

        dStatus.setText("No doctor selected");

    }

    /**
     * Apply consistent styling to the Patients and Doctors modules.
     * UIStyle.styleTree() applies the shared theme first; this method then
     * restores meaningful button colors and configures both data tables.
     */
    private void applyModuleStyling() {
        UIStyle.styleTable(pTable);
        UIStyle.styleTable(dTable);

        pTable.setRowHeight(30);
        dTable.setRowHeight(30);
        pTable.setFillsViewportHeight(true);
        dTable.setFillsViewportHeight(true);
        pTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        dTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        styleModuleButtons(this);
    }

    private void styleModuleButtons(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button) {
                String label = button.getText();

                if ("Register Patient".equals(label) || "Add Doctor".equals(label)) {
                    UIStyle.styleButton(button, UIStyle.BLUE);
                } else if ("Update Selected".equals(label)) {
                    UIStyle.styleButton(button, UIStyle.TEAL);
                } else if ("Clear".equals(label)) {
                    UIStyle.styleButton(button, UIStyle.NAVY);
                } else if ("Search".equals(label)) {
                    UIStyle.styleButton(button, new Color(82, 101, 122));
                } else if ("Show All".equals(label)) {
                    UIStyle.styleButton(button, UIStyle.BLUE);
                }
            }

            if (component instanceof Container container) {
                styleModuleButtons(container);
            }
        }
    }

    // ================= COMMON METHODS =================

    private GridBagConstraints constraints() {

        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(4, 6, 4, 6);

        c.anchor = GridBagConstraints.WEST;

        c.fill = GridBagConstraints.HORIZONTAL;

        return c;

    }

    private void addField(JPanel panel, GridBagConstraints c,

                          int x, int y, String label,

                          JComponent field) {

        c.gridx = x;

        c.gridy = y;

        c.gridwidth = 1;

        c.weightx = 0;

        panel.add(new JLabel(label), c);

        c.gridx = x + 1;

        c.weightx = 1;

        panel.add(field, c);

    }

    private String cell(DefaultTableModel model, int row, int column) {

        Object value = model.getValueAt(row, column);

        return value == null ? "" : value.toString();

    }

    private void warn(String message) {

        JOptionPane.showMessageDialog(

                this, message, "Please check",

                JOptionPane.WARNING_MESSAGE);

    }

    private void databaseError(SQLException ex) {

        String message = ex.getMessage() == null

                ? "Unknown database error."

                : ex.getMessage();

        if (message.contains("ORA-00001")) {

            message = "This phone number is already registered.";

        } else if (message.contains("ORA-00942")) {

            message = "Table not found. Run the required SQL schema script.";

        }

        JOptionPane.showMessageDialog(

                this, message, "Database Error",

                JOptionPane.ERROR_MESSAGE);

    }

}
