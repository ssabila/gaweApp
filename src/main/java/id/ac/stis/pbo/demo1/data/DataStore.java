package id.ac.stis.pbo.demo1.data;

import id.ac.stis.pbo.demo1.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

/**
 * Enhanced in-memory data store with proper role interactions
 * Thread-safe implementation using concurrent collections
 */
public class DataStore {
    // Thread-safe collections for multi-user access
    private static final Map<String, Employee> employees = new ConcurrentHashMap<>();
    private static final List<KPI> kpiList = Collections.synchronizedList(new ArrayList<>());
    private static final List<Report> reportList = Collections.synchronizedList(new ArrayList<>());
    private static final List<EmployeeEvaluation> evaluationList = Collections.synchronizedList(new ArrayList<>());
    private static final List<Attendance> attendanceList = Collections.synchronizedList(new ArrayList<>());
    private static final List<Meeting> meetingList = Collections.synchronizedList(new ArrayList<>());
    private static final List<LeaveRequest> leaveRequestList = Collections.synchronizedList(new ArrayList<>());
    private static final List<SalaryHistory> salaryHistoryList = Collections.synchronizedList(new ArrayList<>());
    private static final List<MonthlyEvaluation> monthlyEvaluationList = Collections.synchronizedList(new ArrayList<>());

    // ID generators
    private static final AtomicInteger reportIdGenerator = new AtomicInteger(1);
    private static final AtomicInteger kpiIdGenerator = new AtomicInteger(1);
    private static final AtomicInteger evaluationIdGenerator = new AtomicInteger(1);
    private static final AtomicInteger attendanceIdGenerator = new AtomicInteger(1);
    private static final AtomicInteger meetingIdGenerator = new AtomicInteger(1);
    private static final AtomicInteger leaveRequestIdGenerator = new AtomicInteger(1);
    private static final AtomicInteger salaryHistoryIdGenerator = new AtomicInteger(1);
    private static final AtomicInteger monthlyEvaluationIdGenerator = new AtomicInteger(1);

    // Monthly Evaluation class
    public static class MonthlyEvaluation {
        private int id;
        private String employeeId;
        private String supervisorId;
        private int month;
        private int year;
        private double punctualityScore;
        private double attendanceScore;
        private double productivityScore;
        private double overallRating;
        private String comments;
        private Date evaluationDate;

        // Constructors and getters/setters
        public MonthlyEvaluation() {}

        public MonthlyEvaluation(String employeeId, String supervisorId, int month, int year,
                                 double punctualityScore, double attendanceScore, double productivityScore,
                                 double overallRating, String comments) {
            this.employeeId = employeeId;
            this.supervisorId = supervisorId;
            this.month = month;
            this.year = year;
            this.punctualityScore = punctualityScore;
            this.attendanceScore = attendanceScore;
            this.productivityScore = productivityScore;
            this.overallRating = overallRating;
            this.comments = comments;
            this.evaluationDate = new Date();
        }

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getSupervisorId() { return supervisorId; }
        public void setSupervisorId(String supervisorId) { this.supervisorId = supervisorId; }
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public double getPunctualityScore() { return punctualityScore; }
        public void setPunctualityScore(double punctualityScore) { this.punctualityScore = punctualityScore; }
        public double getAttendanceScore() { return attendanceScore; }
        public void setAttendanceScore(double attendanceScore) { this.attendanceScore = attendanceScore; }
        public double getProductivityScore() { return productivityScore; }
        public void setProductivityScore(double productivityScore) { this.productivityScore = productivityScore; }
        public double getOverallRating() { return overallRating; }
        public void setOverallRating(double overallRating) { this.overallRating = overallRating; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public Date getEvaluationDate() { return evaluationDate; }
        public void setEvaluationDate(Date evaluationDate) { this.evaluationDate = evaluationDate; }
    }

    /**
     * Initialize the data store with sample data
     */
    public static void initialize() {
        createSampleEmployees();
        createSampleKPI();
        createSampleReports();
        createSampleEvaluations();
        createSampleAttendance();
        createSampleMeetings();
        createSampleLeaveRequests();
        createSampleSalaryHistory();
        createSampleMonthlyEvaluations();
        System.out.println("DataStore initialized with sample data");
    }

    private static void createSampleEmployees() {
        // Create sample employees for all divisions
        Employee[] sampleEmployees = {
                // Managers
                new Employee("MNG001", "John Manager", "password123", "manajer", "HR", "General Manager", new Date()),

                // Supervisors
                new Employee("SUP001", "Alice Supervisor", "password123", "supervisor", "HR", "HR Supervisor", new Date()),
                new Employee("SUP002", "Bob Supervisor", "password123", "supervisor", "Marketing", "Marketing Supervisor", new Date()),
                new Employee("SUP003", "Carol Supervisor", "password123", "supervisor", "Sales", "Sales Supervisor", new Date()),
                new Employee("SUP004", "David Supervisor", "password123", "supervisor", "IT", "IT Supervisor", new Date()),
                new Employee("SUP005", "Eva Supervisor", "password123", "supervisor", "Finance", "Finance Supervisor", new Date()),

                // Employees
                new Employee("EMP001", "Mike Employee", "password123", "pegawai", "Marketing", "Marketing Specialist", new Date()),
                new Employee("EMP002", "Sarah Employee", "password123", "pegawai", "Sales", "Sales Representative", new Date()),
                new Employee("EMP003", "Tom Employee", "password123", "pegawai", "IT", "Software Developer", new Date()),
                new Employee("EMP004", "Lisa Employee", "password123", "pegawai", "Finance", "Financial Analyst", new Date()),
                new Employee("EMP005", "James Employee", "password123", "pegawai", "HR", "HR Assistant", new Date()),
                new Employee("EMP006", "Anna Employee", "password123", "pegawai", "Marketing", "Marketing Assistant", new Date()),
                new Employee("EMP007", "Peter Employee", "password123", "pegawai", "Sales", "Sales Assistant", new Date()),
                new Employee("EMP008", "Maria Employee", "password123", "pegawai", "IT", "System Analyst", new Date()),
                new Employee("EMP009", "Robert Employee", "password123", "pegawai", "Finance", "Accountant", new Date()),
                new Employee("EMP010", "Linda Employee", "password123", "pegawai", "HR", "Recruiter", new Date())
        };

        for (Employee emp : sampleEmployees) {
            // Set initial performance scores
            emp.setKpiScore(60 + Math.random() * 35); // Random score between 60-95
            emp.setSupervisorRating(65 + Math.random() * 30); // Random rating between 65-95
            emp.setLayoffRisk(emp.getKpiScore() < 60 || emp.getSupervisorRating() < 60);
            employees.put(emp.getId(), emp);
        }
    }

    private static void createSampleKPI() {
        String[] divisions = {"HR", "Marketing", "Sales", "IT", "Finance"};

        for (String division : divisions) {
            // Create KPI for last 6 months
            for (int month = 7; month <= 12; month++) {
                KPI kpi = new KPI();
                kpi.setId(kpiIdGenerator.getAndIncrement());
                kpi.setDivisi(division);
                kpi.setBulan(month);
                kpi.setTahun(2024);
                kpi.setScore(60 + Math.random() * 35); // Random score between 60-95
                kpi.setManagerId("MNG001");
                kpi.setCreatedDate(new Date());
                kpiList.add(kpi);
            }
        }
    }

    private static void createSampleReports() {
        String[] divisions = {"HR", "Marketing", "Sales", "IT", "Finance"};
        String[] supervisorIds = {"SUP001", "SUP002", "SUP003", "SUP004", "SUP005"};

        for (int i = 0; i < divisions.length; i++) {
            // Create reports for last 3 months
            for (int month = 10; month <= 12; month++) {
                Report report = new Report();
                report.setId(reportIdGenerator.getAndIncrement());
                report.setSupervisorId(supervisorIds[i]);
                report.setDivisi(divisions[i]);
                report.setBulan(month);
                report.setTahun(2024);
                report.setFilePath("reports/" + divisions[i] + "_" + month + "_2024.pdf");
                report.setUploadDate(new Date());
                report.setStatus(month == 12 ? "pending" : "reviewed");
                if (!report.getStatus().equals("pending")) {
                    report.setManagerNotes("Good performance this month");
                    report.setReviewedBy("MNG001");
                    report.setReviewedDate(new Date());
                }
                reportList.add(report);
            }
        }
    }

    private static void createSampleEvaluations() {
        // Create evaluations for employees
        List<Employee> employeeList = getEmployeesByRole("pegawai");

        for (Employee emp : employeeList) {
            EmployeeEvaluation eval = new EmployeeEvaluation();
            eval.setId(evaluationIdGenerator.getAndIncrement());
            eval.setEmployeeId(emp.getId());
            eval.setSupervisorId(getSupervisorByDivision(emp.getDivisi()));
            eval.setPunctualityScore(70 + Math.random() * 25);
            eval.setAttendanceScore(75 + Math.random() * 20);
            eval.setOverallRating((eval.getPunctualityScore() + eval.getAttendanceScore()) / 2);
            eval.setEvaluationDate(new Date());
            eval.setComments("Regular evaluation for " + emp.getNama());
            evaluationList.add(eval);

            // Update employee supervisor rating
            emp.setSupervisorRating(eval.getOverallRating());
        }
    }

    private static void createSampleMonthlyEvaluations() {
        // Create sample monthly evaluations
        List<Employee> employeeList = getEmployeesByRole("pegawai");

        for (Employee emp : employeeList) {
            // Create evaluations for last 3 months
            for (int month = 10; month <= 12; month++) {
                MonthlyEvaluation eval = new MonthlyEvaluation();
                eval.setId(monthlyEvaluationIdGenerator.getAndIncrement());
                eval.setEmployeeId(emp.getId());
                eval.setSupervisorId(getSupervisorByDivision(emp.getDivisi()));
                eval.setMonth(month);
                eval.setYear(2024);
                eval.setPunctualityScore(70 + Math.random() * 25);
                eval.setAttendanceScore(75 + Math.random() * 20);
                eval.setProductivityScore(70 + Math.random() * 25);
                eval.setOverallRating((eval.getPunctualityScore() + eval.getAttendanceScore() + eval.getProductivityScore()) / 3);
                eval.setComments("Monthly evaluation for " + emp.getNama() + " - Month " + month);
                eval.setEvaluationDate(new Date());
                monthlyEvaluationList.add(eval);
            }
        }
    }

    private static void createSampleAttendance() {
        List<Employee> allEmployees = getAllEmployees();
        Calendar cal = Calendar.getInstance();

        // Create attendance for last 30 days
        for (int day = 1; day <= 30; day++) {
            cal.set(2024, Calendar.DECEMBER, day);
            Date attendanceDate = cal.getTime();

            for (Employee emp : allEmployees) {
                if (Math.random() > 0.1) { // 90% attendance rate
                    Attendance attendance = new Attendance();
                    attendance.setId(attendanceIdGenerator.getAndIncrement());
                    attendance.setEmployeeId(emp.getId());
                    attendance.setTanggal(attendanceDate);
                    attendance.setJamMasuk("08:" + String.format("%02d", (int)(Math.random() * 60)));
                    attendance.setJamKeluar("17:" + String.format("%02d", (int)(Math.random() * 60)));
                    attendance.setStatus("hadir");
                    attendance.setLate(Math.random() > 0.8); // 20% chance of being late
                    attendanceList.add(attendance);
                }
            }
        }
    }

    private static void createSampleMeetings() {
        Calendar cal = Calendar.getInstance();

        // Create sample meetings for next 2 weeks
        for (int day = 1; day <= 14; day++) {
            cal.set(2024, Calendar.DECEMBER, day + 15);
            Date meetingDate = cal.getTime();

            // Weekly team meetings
            if (day % 7 == 1) {
                String[] divisions = {"HR", "Marketing", "Sales", "IT", "Finance"};
                String[] supervisorIds = {"SUP001", "SUP002", "SUP003", "SUP004", "SUP005"};

                for (int i = 0; i < divisions.length; i++) {
                    Meeting meeting = new Meeting();
                    meeting.setId(meetingIdGenerator.getAndIncrement());
                    meeting.setTitle(divisions[i] + " Weekly Team Meeting");
                    meeting.setDescription("Weekly team sync and updates");
                    meeting.setTanggal(meetingDate);
                    meeting.setWaktuMulai("09:00");
                    meeting.setWaktuSelesai("10:00");
                    meeting.setLokasi("Meeting Room " + (i + 1));
                    meeting.setOrganizerId(supervisorIds[i]);

                    // Add team members as participants
                    List<String> participants = getEmployeesByDivision(divisions[i])
                            .stream()
                            .map(Employee::getId)
                            .collect(Collectors.toList());
                    meeting.setParticipantIds(participants);
                    meeting.setStatus("scheduled");
                    meeting.setCreatedDate(new Date());
                    meetingList.add(meeting);
                }
            }

            // Monthly all-hands meeting
            if (day == 7) {
                Meeting allHands = new Meeting();
                allHands.setId(meetingIdGenerator.getAndIncrement());
                allHands.setTitle("Monthly All-Hands Meeting");
                allHands.setDescription("Company updates and announcements");
                allHands.setTanggal(meetingDate);
                allHands.setWaktuMulai("14:00");
                allHands.setWaktuSelesai("15:30");
                allHands.setLokasi("Main Conference Room");
                allHands.setOrganizerId("MNG001");

                List<String> allParticipants = getAllEmployees()
                        .stream()
                        .map(Employee::getId)
                        .collect(Collectors.toList());
                allHands.setParticipantIds(allParticipants);
                allHands.setStatus("scheduled");
                allHands.setCreatedDate(new Date());
                meetingList.add(allHands);
            }
        }
    }

    private static void createSampleLeaveRequests() {
        List<Employee> allEmployees = getAllEmployees();
        Calendar cal = Calendar.getInstance();

        // Create some sample leave requests with proper role hierarchy
        for (int i = 0; i < 15; i++) {
            Employee emp = allEmployees.get(i % allEmployees.size());

            cal.set(2024, Calendar.DECEMBER, 20 + (i % 10));
            Date startDate = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, 2 + (i % 3));
            Date endDate = cal.getTime();

            LeaveRequest leaveRequest = new LeaveRequest();
            leaveRequest.setId(leaveRequestIdGenerator.getAndIncrement());
            leaveRequest.setEmployeeId(emp.getId());
            leaveRequest.setLeaveType(i % 4 == 0 ? "Annual Leave" : i % 4 == 1 ? "Sick Leave" : i % 4 == 2 ? "Personal Leave" : "Emergency Leave");
            leaveRequest.setStartDate(startDate);
            leaveRequest.setEndDate(endDate);
            leaveRequest.setTotalDays(3 + (i % 3));
            leaveRequest.setReason("Sample leave request " + (i + 1));
            
            // Set status based on role and create proper approval hierarchy
            if (i < 5) {
                leaveRequest.setStatus("pending");
            } else if (i < 10) {
                leaveRequest.setStatus("approved");
                // Set appropriate approver based on employee role
                if (emp.getRole().equals("pegawai")) {
                    leaveRequest.setApproverId(getSupervisorByDivision(emp.getDivisi()));
                } else if (emp.getRole().equals("supervisor")) {
                    leaveRequest.setApproverId("MNG001");
                }
                leaveRequest.setApproverNotes("Approved - good standing employee");
                leaveRequest.setApprovalDate(new Date());
            } else {
                leaveRequest.setStatus("rejected");
                if (emp.getRole().equals("pegawai")) {
                    leaveRequest.setApproverId(getSupervisorByDivision(emp.getDivisi()));
                } else if (emp.getRole().equals("supervisor")) {
                    leaveRequest.setApproverId("MNG001");
                }
                leaveRequest.setApproverNotes("Rejected due to workload constraints");
                leaveRequest.setApprovalDate(new Date());
            }
            
            leaveRequest.setRequestDate(new Date());
            leaveRequestList.add(leaveRequest);
        }
    }

    private static void createSampleSalaryHistory() {
        List<Employee> allEmployees = getAllEmployees();

        // Create salary history for last 6 months
        for (Employee emp : allEmployees) {
            for (int month = 7; month <= 12; month++) {
                SalaryHistory salary = new SalaryHistory();
                salary.setId(salaryHistoryIdGenerator.getAndIncrement());
                salary.setEmployeeId(emp.getId());
                salary.setBulan(month);
                salary.setTahun(2024);
                salary.setBaseSalary(emp.getGajiPokok());

                // Calculate bonuses and penalties
                double kpiBonus = 0;
                double supervisorBonus = 0;
                double penalty = 0;

                if (emp.getKpiScore() >= 90) kpiBonus = emp.getGajiPokok() * 0.20;
                else if (emp.getKpiScore() >= 80) kpiBonus = emp.getGajiPokok() * 0.15;
                else if (emp.getKpiScore() >= 70) kpiBonus = emp.getGajiPokok() * 0.10;
                else if (emp.getKpiScore() >= 60) kpiBonus = emp.getGajiPokok() * 0.05;

                if (emp.getSupervisorRating() >= 90) supervisorBonus = emp.getGajiPokok() * 0.15;
                else if (emp.getSupervisorRating() >= 80) supervisorBonus = emp.getGajiPokok() * 0.10;
                else if (emp.getSupervisorRating() >= 70) supervisorBonus = emp.getGajiPokok() * 0.05;

                if (emp.getKpiScore() < 60 || emp.getSupervisorRating() < 60) {
                    penalty = emp.getGajiPokok() * 0.10;
                }

                salary.setKpiBonus(kpiBonus);
                salary.setSupervisorBonus(supervisorBonus);
                salary.setPenalty(penalty);
                salary.setTotalSalary(emp.getGajiPokok() + kpiBonus + supervisorBonus - penalty);
                salary.setKpiScore(emp.getKpiScore());
                salary.setSupervisorRating(emp.getSupervisorRating());
                salary.setPaymentDate(new Date());
                salaryHistoryList.add(salary);
            }
        }
    }

    // Authentication
    public static Employee authenticateUser(String employeeId, String password) {
        Employee emp = employees.get(employeeId);
        if (emp != null && emp.getPassword().equals(password)) {
            return emp;
        }
        return null;
    }

    // Employee operations
    public static List<Employee> getAllEmployees() {
        return new ArrayList<>(employees.values());
    }

    public static List<Employee> getEmployeesByRole(String role) {
        return employees.values().stream()
                .filter(emp -> emp.getRole().equalsIgnoreCase(role))
                .collect(Collectors.toList());
    }

    public static List<Employee> getEmployeesByDivision(String divisi) {
        return employees.values().stream()
                .filter(emp -> emp.getDivisi().equals(divisi))
                .collect(Collectors.toList());
    }

    public static Employee getEmployeeById(String id) {
        return employees.get(id);
    }

    public static void updateEmployee(Employee employee) {
        employees.put(employee.getId(), employee);
    }

    // KPI operations
    public static List<KPI> getAllKPI() {
        return new ArrayList<>(kpiList);
    }

    public static List<KPI> getKPIByDivision(String divisi) {
        return kpiList.stream()
                .filter(kpi -> kpi.getDivisi().equals(divisi))
                .sorted((k1, k2) -> {
                    int yearCompare = Integer.compare(k2.getTahun(), k1.getTahun());
                    if (yearCompare != 0) return yearCompare;
                    return Integer.compare(k2.getBulan(), k1.getBulan());
                })
                .collect(Collectors.toList());
    }

    public static boolean saveKPI(String divisi, int bulan, int tahun, double score, String managerId) {
        // Remove existing KPI for same period
        kpiList.removeIf(kpi -> kpi.getDivisi().equals(divisi) &&
                kpi.getBulan() == bulan &&
                kpi.getTahun() == tahun);

        // Add new KPI
        KPI kpi = new KPI();
        kpi.setId(kpiIdGenerator.getAndIncrement());
        kpi.setDivisi(divisi);
        kpi.setBulan(bulan);
        kpi.setTahun(tahun);
        kpi.setScore(score);
        kpi.setManagerId(managerId);
        kpi.setCreatedDate(new Date());
        kpiList.add(kpi);

        // Update employee layoff risk and KPI scores
        updateEmployeeKPIScores(divisi, score);

        return true;
    }

    private static void updateEmployeeKPIScores(String divisi, double kpiScore) {
        employees.values().stream()
                .filter(emp -> emp.getDivisi().equals(divisi) && emp.getRole().equals("pegawai"))
                .forEach(emp -> {
                    emp.setKpiScore(kpiScore);
                    emp.setLayoffRisk(kpiScore < 60 || emp.getSupervisorRating() < 60);
                });
    }

    // Report operations
    public static List<Report> getAllReports() {
        return new ArrayList<>(reportList);
    }

    public static List<Report> getPendingReports() {
        return reportList.stream()
                .filter(report -> "pending".equals(report.getStatus()))
                .sorted((r1, r2) -> r2.getUploadDate().compareTo(r1.getUploadDate()))
                .collect(Collectors.toList());
    }

    public static List<Report> getReportsByDivision(String divisi) {
        return reportList.stream()
                .filter(report -> report.getDivisi().equals(divisi))
                .sorted((r1, r2) -> r2.getUploadDate().compareTo(r1.getUploadDate()))
                .collect(Collectors.toList());
    }

    public static boolean saveReport(String supervisorId, String divisi, int bulan, int tahun, String filePath) {
        Report report = new Report();
        report.setId(reportIdGenerator.getAndIncrement());
        report.setSupervisorId(supervisorId);
        report.setDivisi(divisi);
        report.setBulan(bulan);
        report.setTahun(tahun);
        report.setFilePath(filePath);
        report.setUploadDate(new Date());
        report.setStatus("pending");
        reportList.add(report);
        return true;
    }

    public static boolean updateReportStatus(int reportId, String status, String managerNotes, String reviewedBy) {
        Report report = reportList.stream()
                .filter(r -> r.getId() == reportId)
                .findFirst()
                .orElse(null);

        if (report != null) {
            report.setStatus(status);
            report.setManagerNotes(managerNotes);
            report.setReviewedBy(reviewedBy);
            report.setReviewedDate(new Date());
            return true;
        }
        return false;
    }

    // Employee Evaluation operations
    public static List<EmployeeEvaluation> getAllEvaluations() {
        return new ArrayList<>(evaluationList);
    }

    public static List<EmployeeEvaluation> getEvaluationsByEmployee(String employeeId) {
        return evaluationList.stream()
                .filter(eval -> eval.getEmployeeId().equals(employeeId))
                .sorted((e1, e2) -> e2.getEvaluationDate().compareTo(e1.getEvaluationDate()))
                .collect(Collectors.toList());
    }

    public static List<EmployeeEvaluation> getEvaluationsBySupervisor(String supervisorId) {
        return evaluationList.stream()
                .filter(eval -> eval.getSupervisorId().equals(supervisorId))
                .sorted((e1, e2) -> e2.getEvaluationDate().compareTo(e1.getEvaluationDate()))
                .collect(Collectors.toList());
    }

    public static boolean saveEmployeeEvaluation(String employeeId, String supervisorId,
                                                 double punctualityScore, double attendanceScore,
                                                 double overallRating, String comments) {
        EmployeeEvaluation eval = new EmployeeEvaluation();
        eval.setId(evaluationIdGenerator.getAndIncrement());
        eval.setEmployeeId(employeeId);
        eval.setSupervisorId(supervisorId);
        eval.setPunctualityScore(punctualityScore);
        eval.setAttendanceScore(attendanceScore);
        eval.setOverallRating(overallRating);
        eval.setComments(comments);
        eval.setEvaluationDate(new Date());
        evaluationList.add(eval);

        // Update employee supervisor rating
        Employee employee = employees.get(employeeId);
        if (employee != null) {
            employee.setSupervisorRating(overallRating);
            employee.setLayoffRisk(employee.getKpiScore() < 60 || overallRating < 60);
        }

        return true;
    }

    // Monthly Evaluation operations
    public static boolean hasMonthlyEvaluation(String employeeId, int month, int year) {
        return monthlyEvaluationList.stream()
                .anyMatch(eval -> eval.getEmployeeId().equals(employeeId) &&
                        eval.getMonth() == month &&
                        eval.getYear() == year);
    }

    public static boolean saveMonthlyEmployeeEvaluation(String employeeId, String supervisorId,
                                                        int month, int year,
                                                        double punctualityScore, double attendanceScore,
                                                        double productivityScore, double overallRating,
                                                        String comments) {
        MonthlyEvaluation eval = new MonthlyEvaluation();
        eval.setId(monthlyEvaluationIdGenerator.getAndIncrement());
        eval.setEmployeeId(employeeId);
        eval.setSupervisorId(supervisorId);
        eval.setMonth(month);
        eval.setYear(year);
        eval.setPunctualityScore(punctualityScore);
        eval.setAttendanceScore(attendanceScore);
        eval.setProductivityScore(productivityScore);
        eval.setOverallRating(overallRating);
        eval.setComments(comments);
        eval.setEvaluationDate(new Date());
        monthlyEvaluationList.add(eval);

        // Update employee supervisor rating with latest monthly evaluation
        Employee employee = employees.get(employeeId);
        if (employee != null) {
            employee.setSupervisorRating(overallRating);
            employee.setLayoffRisk(employee.getKpiScore() < 60 || overallRating < 60);
        }

        return true;
    }

    public static List<MonthlyEvaluation> getAllMonthlyEvaluations() {
        return new ArrayList<>(monthlyEvaluationList);
    }

    public static List<MonthlyEvaluation> getMonthlyEvaluationsByEmployee(String employeeId) {
        return monthlyEvaluationList.stream()
                .filter(eval -> eval.getEmployeeId().equals(employeeId))
                .sorted((e1, e2) -> {
                    int yearCompare = Integer.compare(e2.getYear(), e1.getYear());
                    if (yearCompare != 0) return yearCompare;
                    return Integer.compare(e2.getMonth(), e1.getMonth());
                })
                .collect(Collectors.toList());
    }

    public static List<MonthlyEvaluation> getMonthlyEvaluationsBySupervisor(String supervisorId) {
        return monthlyEvaluationList.stream()
                .filter(eval -> eval.getSupervisorId().equals(supervisorId))
                .sorted((e1, e2) -> {
                    int yearCompare = Integer.compare(e2.getYear(), e1.getYear());
                    if (yearCompare != 0) return yearCompare;
                    return Integer.compare(e2.getMonth(), e1.getMonth());
                })
                .collect(Collectors.toList());
    }

    // Attendance operations
    public static List<Attendance> getAllAttendance() {
        return new ArrayList<>(attendanceList);
    }

    public static List<Attendance> getAttendanceByEmployee(String employeeId) {
        return attendanceList.stream()
                .filter(att -> att.getEmployeeId().equals(employeeId))
                .sorted((a1, a2) -> a2.getTanggal().compareTo(a1.getTanggal()))
                .collect(Collectors.toList());
    }

    // Get today's attendance for an employee
    public static List<Attendance> getTodayAttendance(String employeeId) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTime(today.getTime());
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        return attendanceList.stream()
                .filter(att -> att.getEmployeeId().equals(employeeId))
                .filter(att -> att.getTanggal().compareTo(today.getTime()) >= 0 &&
                        att.getTanggal().compareTo(tomorrow.getTime()) < 0)
                .collect(Collectors.toList());
    }

    public static boolean saveAttendance(String employeeId, Date tanggal, String jamMasuk, String jamKeluar, String status) {
        Attendance attendance = new Attendance();
        attendance.setId(attendanceIdGenerator.getAndIncrement());
        attendance.setEmployeeId(employeeId);
        attendance.setTanggal(tanggal);
        attendance.setJamMasuk(jamMasuk);
        attendance.setJamKeluar(jamKeluar);
        attendance.setStatus(status);
        attendance.setLate(isLateArrival(jamMasuk));
        attendanceList.add(attendance);
        return true;
    }

    // Update clock out time for today's attendance
    public static boolean updateAttendanceClockOut(String employeeId, String jamKeluar) {
        List<Attendance> todayAttendance = getTodayAttendance(employeeId);

        if (!todayAttendance.isEmpty()) {
            Attendance attendance = todayAttendance.get(0);
            attendance.setJamKeluar(jamKeluar);
            return true;
        }

        return false;
    }

    private static boolean isLateArrival(String jamMasuk) {
        if (jamMasuk == null) return false;
        try {
            String[] parts = jamMasuk.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour > 8 || (hour == 8 && minute > 30); // Late if after 08:30
        } catch (Exception e) {
            return false;
        }
    }

    // Meeting operations
    public static List<Meeting> getAllMeetings() {
        return new ArrayList<>(meetingList);
    }

    public static List<Meeting> getMeetingsByEmployee(String employeeId) {
        return meetingList.stream()
                .filter(meeting -> meeting.getOrganizerId().equals(employeeId) ||
                        (meeting.getParticipantIds() != null && meeting.getParticipantIds().contains(employeeId)))
                .sorted((m1, m2) -> m1.getTanggal().compareTo(m2.getTanggal()))
                .collect(Collectors.toList());
    }

    public static List<Meeting> getUpcomingMeetings() {
        Date today = new Date();
        return meetingList.stream()
                .filter(meeting -> meeting.getTanggal().after(today) ||
                        meeting.getTanggal().equals(today))
                .sorted((m1, m2) -> m1.getTanggal().compareTo(m2.getTanggal()))
                .collect(Collectors.toList());
    }

    public static boolean saveMeeting(String title, String description, Date tanggal, String waktuMulai,
                                      String waktuSelesai, String lokasi, String organizerId, List<String> participantIds) {
        Meeting meeting = new Meeting();
        meeting.setId(meetingIdGenerator.getAndIncrement());
        meeting.setTitle(title);
        meeting.setDescription(description);
        meeting.setTanggal(tanggal);
        meeting.setWaktuMulai(waktuMulai);
        meeting.setWaktuSelesai(waktuSelesai);
        meeting.setLokasi(lokasi);
        meeting.setOrganizerId(organizerId);
        meeting.setParticipantIds(participantIds);
        meeting.setStatus("scheduled");
        meeting.setCreatedDate(new Date());
        meetingList.add(meeting);
        return true;
    }

    public static boolean updateMeetingStatus(int meetingId, String status) {
        Meeting meeting = meetingList.stream()
                .filter(m -> m.getId() == meetingId)
                .findFirst()
                .orElse(null);

        if (meeting != null) {
            meeting.setStatus(status);
            return true;
        }
        return false;
    }

    // Leave Request operations with proper role hierarchy
    public static List<LeaveRequest> getAllLeaveRequests() {
        return new ArrayList<>(leaveRequestList);
    }

    public static List<LeaveRequest> getLeaveRequestsByEmployee(String employeeId) {
        return leaveRequestList.stream()
                .filter(leave -> leave.getEmployeeId().equals(employeeId))
                .sorted((l1, l2) -> l2.getRequestDate().compareTo(l1.getRequestDate()))
                .collect(Collectors.toList());
    }

    public static List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestList.stream()
                .filter(leave -> "pending".equals(leave.getStatus()))
                .sorted((l1, l2) -> l1.getRequestDate().compareTo(l2.getRequestDate()))
                .collect(Collectors.toList());
    }

    /**
     * Get leave requests that need approval by a specific approver
     * Employees' requests go to their division supervisor
     * Supervisors' requests go to manager
     */
    public static List<LeaveRequest> getLeaveRequestsForApproval(String approverId) {
        Employee approver = employees.get(approverId);
        if (approver == null) return new ArrayList<>();

        return leaveRequestList.stream()
                .filter(leave -> "pending".equals(leave.getStatus()))
                .filter(leave -> {
                    Employee requester = employees.get(leave.getEmployeeId());
                    if (requester == null) return false;

                    // Supervisors approve employees in their division
                    if (approver.getRole().equals("supervisor")) {
                        return requester.getRole().equals("pegawai") &&
                                requester.getDivisi().equals(approver.getDivisi());
                    }
                    // Managers approve supervisors and can approve any employee
                    else if (approver.getRole().equals("manajer")) {
                        return requester.getRole().equals("supervisor") ||
                               requester.getRole().equals("pegawai");
                    }
                    return false;
                })
                .sorted((l1, l2) -> l1.getRequestDate().compareTo(l2.getRequestDate()))
                .collect(Collectors.toList());
    }

    public static boolean saveLeaveRequest(String employeeId, String leaveType, Date startDate, Date endDate, String reason) {
        // Calculate total days
        LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(leaveRequestIdGenerator.getAndIncrement());
        leaveRequest.setEmployeeId(employeeId);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setTotalDays(totalDays);
        leaveRequest.setReason(reason);
        leaveRequest.setStatus("pending");
        leaveRequest.setRequestDate(new Date());
        leaveRequestList.add(leaveRequest);
        return true;
    }

    public static boolean approveLeaveRequest(int leaveRequestId, String approverId, String notes) {
        LeaveRequest leaveRequest = leaveRequestList.stream()
                .filter(l -> l.getId() == leaveRequestId)
                .findFirst()
                .orElse(null);

        if (leaveRequest != null) {
            leaveRequest.setStatus("approved");
            leaveRequest.setApproverId(approverId);
            leaveRequest.setApproverNotes(notes);
            leaveRequest.setApprovalDate(new Date());

            // Deduct leave days from employee
            Employee employee = employees.get(leaveRequest.getEmployeeId());
            if (employee != null) {
                employee.setSisaCuti(employee.getSisaCuti() - leaveRequest.getTotalDays());
            }

            return true;
        }
        return false;
    }

    public static boolean rejectLeaveRequest(int leaveRequestId, String approverId, String notes) {
        LeaveRequest leaveRequest = leaveRequestList.stream()
                .filter(l -> l.getId() == leaveRequestId)
                .findFirst()
                .orElse(null);

        if (leaveRequest != null) {
            leaveRequest.setStatus("rejected");
            leaveRequest.setApproverId(approverId);
            leaveRequest.setApproverNotes(notes);
            leaveRequest.setApprovalDate(new Date());
            return true;
        }
        return false;
    }

    // Salary History operations
    public static List<SalaryHistory> getAllSalaryHistory() {
        return new ArrayList<>(salaryHistoryList);
    }

    public static List<SalaryHistory> getSalaryHistoryByEmployee(String employeeId) {
        return salaryHistoryList.stream()
                .filter(salary -> salary.getEmployeeId().equals(employeeId))
                .sorted((s1, s2) -> {
                    int yearCompare = Integer.compare(s2.getTahun(), s1.getTahun());
                    if (yearCompare != 0) return yearCompare;
                    return Integer.compare(s2.getBulan(), s1.getBulan());
                })
                .collect(Collectors.toList());
    }

    public static boolean saveSalaryHistory(String employeeId, int bulan, int tahun, double baseSalary,
                                            double kpiBonus, double supervisorBonus, double penalty,
                                            double totalSalary, double kpiScore, double supervisorRating) {
        SalaryHistory salary = new SalaryHistory();
        salary.setId(salaryHistoryIdGenerator.getAndIncrement());
        salary.setEmployeeId(employeeId);
        salary.setBulan(bulan);
        salary.setTahun(tahun);
        salary.setBaseSalary(baseSalary);
        salary.setKpiBonus(kpiBonus);
        salary.setSupervisorBonus(supervisorBonus);
        salary.setPenalty(penalty);
        salary.setTotalSalary(totalSalary);
        salary.setKpiScore(kpiScore);
        salary.setSupervisorRating(supervisorRating);
        salary.setPaymentDate(new Date());
        salaryHistoryList.add(salary);
        return true;
    }

    // Utility methods
    public static String getSupervisorByDivision(String divisi) {
        return employees.values().stream()
                .filter(emp -> emp.getRole().equals("supervisor") && emp.getDivisi().equals(divisi))
                .map(Employee::getId)
                .findFirst()
                .orElse("SUP001");
    }

    public static Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Employee> allEmployees = getAllEmployees();
        stats.put("totalEmployees", allEmployees.size());
        stats.put("totalManagers", getEmployeesByRole("manajer").size());
        stats.put("totalSupervisors", getEmployeesByRole("supervisor").size());
        stats.put("totalEmployeesRegular", getEmployeesByRole("pegawai").size());

        long layoffRiskCount = allEmployees.stream()
                .filter(Employee::isLayoffRisk)
                .count();
        stats.put("layoffRiskEmployees", layoffRiskCount);

        stats.put("pendingReports", getPendingReports().size());
        stats.put("pendingLeaveRequests", getPendingLeaveRequests().size());
        stats.put("upcomingMeetings", getUpcomingMeetings().size());

        // Average KPI by division
        Map<String, Double> avgKpiByDivision = new HashMap<>();
        String[] divisions = {"HR", "Marketing", "Sales", "IT", "Finance"};
        for (String division : divisions) {
            List<KPI> divisionKpis = getKPIByDivision(division);
            if (!divisionKpis.isEmpty()) {
                double avgKpi = divisionKpis.stream()
                        .mapToDouble(KPI::getScore)
                        .average()
                        .orElse(0.0);
                avgKpiByDivision.put(division, avgKpi);
            }
        }
        stats.put("avgKpiByDivision", avgKpiByDivision);

        return stats;
    }

    // Clear all data (for testing)
    public static void clearAllData() {
        employees.clear();
        kpiList.clear();
        reportList.clear();
        evaluationList.clear();
        attendanceList.clear();
        meetingList.clear();
        leaveRequestList.clear();
        salaryHistoryList.clear();
        monthlyEvaluationList.clear();
    }
}