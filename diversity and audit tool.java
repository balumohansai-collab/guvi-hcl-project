import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

 class DiversityInclusionAudit {

    // Node for Employee DLL
    static class EmployeeNode {
        String id;
        String name;
        int age;
        String gender;      // e.g., Male/Female/Non-binary
        String ethnicity;   // free text
        boolean hasDisability;
        EmployeeNode prev, next;

        EmployeeNode(String id, String name, int age, String gender, String ethnicity, boolean hasDisability) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.ethnicity = ethnicity;
            this.hasDisability = hasDisability;
        }
    }

    // DLL managing employees with MongoDB
    static class EmployeeDLL {
        EmployeeNode head, tail;
        MongoCollection<Document> empCollection;
        MongoCollection<Document> auditCollection;
        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_DATE;

        EmployeeDLL(MongoCollection<Document> empCollection, MongoCollection<Document> auditCollection) {
            this.empCollection = empCollection;
            this.auditCollection = auditCollection;
        }

        // Load employees from MongoDB into DLL
        void loadFromDatabase() {
            FindIterable<Document> docs = empCollection.find().sort(Sorts.ascending("id"));
            for (Document doc : docs) {
                String id = doc.getString("id");
                String name = doc.getString("name");
                int age = safeParseInt(doc.get("age"));
                String gender = doc.getString("gender");
                String ethnicity = doc.getString("ethnicity");
                boolean hasDisability = doc.getBoolean("hasDisability", false);

                EmployeeNode newNode = new EmployeeNode(id, name, age, gender, ethnicity, hasDisability);
                if (head == null) head = tail = newNode;
                else {
                    tail.next = newNode;
                    newNode.prev = tail;
                    tail = newNode;
                }
            }
        }

        private int safeParseInt(Object obj) {
            if (obj == null) return 0;
            if (obj instanceof Integer) return (Integer) obj;
            try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
        }

        // Add employee (DLL + Mongo)
        void addEmployee(String id, String name, int age, String gender, String ethnicity, boolean hasDisability) {
            if (searchEmployee(id) != null) {
                System.out.println("Employee with this ID already exists.");
                return;
            }
            EmployeeNode newNode = new EmployeeNode(id, name, age, gender, ethnicity, hasDisability);
            if (head == null) head = tail = newNode;
            else {
                tail.next = newNode;
                newNode.prev = tail;
                tail = newNode;
            }

            Document doc = new Document("id", id)
                    .append("name", name)
                    .append("age", age)
                    .append("gender", gender)
                    .append("ethnicity", ethnicity)
                    .append("hasDisability", hasDisability);
            empCollection.insertOne(doc);
            System.out.println("✅ Employee added.");
        }

        // Display all employees from DLL
        void displayEmployees() {
            if (head == null) {
                System.out.println("No employees found.");
                return;
            }
            System.out.printf("%-10s %-20s %-5s %-12s %-15s %-10s%n", "ID", "Name", "Age", "Gender", "Ethnicity", "Disability");
            EmployeeNode cur = head;
            while (cur != null) {
                System.out.printf("%-10s %-20s %-5d %-12s %-15s %-10s%n",
                        cur.id, cur.name, cur.age, cur.gender, cur.ethnicity, cur.hasDisability ? "Yes" : "No");
                cur = cur.next;
            }
        }

        // Search by ID
        EmployeeNode searchEmployee(String id) {
            EmployeeNode cur = head;
            while (cur != null) {
                if (cur.id.equals(id)) return cur;
                cur = cur.next;
            }
            return null;
        }

        // Update employee
        void updateEmployee(String id, String newName, int newAge, String newGender, String newEthnicity, boolean newDisability) {
            EmployeeNode node = searchEmployee(id);
            if (node == null) {
                System.out.println("Employee not found.");
                return;
            }
            node.name = newName;
            node.age = newAge;
            node.gender = newGender;
            node.ethnicity = newEthnicity;
            node.hasDisability = newDisability;

            empCollection.updateOne(new Document("id", id),
                    new Document("$set", new Document("name", newName)
                            .append("age", newAge)
                            .append("gender", newGender)
                            .append("ethnicity", newEthnicity)
                            .append("hasDisability", newDisability)));
            System.out.println("✅ Employee updated.");
        }

        // Delete employee & related audit records
        void deleteEmployee(String id) {
            EmployeeNode node = searchEmployee(id);
            if (node == null) {
                System.out.println("Employee not found.");
                return;
            }
            if (node.prev != null) node.prev.next = node.next;
            if (node.next != null) node.next.prev = node.prev;
            if (node == head) head = node.next;
            if (node == tail) tail = node.prev;

            empCollection.deleteOne(new Document("id", id));
            auditCollection.deleteMany(new Document("employeeId", id));
            System.out.println("✅ Employee and their audit records deleted.");
        }

        // Add an audit record for an employee
        void addAuditRecord(String employeeId, String auditor, LocalDate date, int inclusionScore, String notes) {
            EmployeeNode node = searchEmployee(employeeId);
            if (node == null) {
                System.out.println("Employee not found. Cannot add audit.");
                return;
            }
            Document auditDoc = new Document("employeeId", employeeId)
                    .append("auditor", auditor)
                    .append("date", dateFmt.format(date))
                    .append("inclusionScore", inclusionScore) // 0-100
                    .append("notes", notes)
                    .append("employeeGender", node.gender)
                    .append("employeeEthnicity", node.ethnicity)
                    .append("employeeHasDisability", node.hasDisability);
            auditCollection.insertOne(auditDoc);
            System.out.println("✅ Audit record saved.");
        }

        // Display audits for a given employee
        void displayAuditsForEmployee(String employeeId) {
            FindIterable<Document> docs = auditCollection.find(new Document("employeeId", employeeId)).sort(Sorts.ascending("date"));
            boolean any = false;
            for (Document d : docs) {
                any = true;
                System.out.println("----");
                System.out.println("Date: " + d.getString("date"));
                System.out.println("Auditor: " + d.getString("auditor"));
                System.out.println("Inclusion Score: " + d.getInteger("inclusionScore", 0));
                System.out.println("Notes: " + d.getString("notes"));
            }
            if (!any) System.out.println("No audit records for employee " + employeeId);
        }

        // Compute basic D&I metrics from employees collection and audits
        void computeMetrics() {
            // Employees stats (iterate DLL)
            int total = 0;
            int male = 0, female = 0, nonBinary = 0, otherGender = 0;
            int disabilityCount = 0;

            EmployeeNode cur = head;
            while (cur != null) {
                total++;
                String g = cur.gender == null ? "" : cur.gender.toLowerCase();
                if (g.contains("male")) male++;
                else if (g.contains("female")) female++;
                else if (g.contains("non")) nonBinary++;
                else otherGender++;
                if (cur.hasDisability) disabilityCount++;
                cur = cur.next;
            }

            System.out.println("=== Employee Diversity Snapshot ===");
            System.out.println("Total employees: " + total);
            if (total > 0) {
                System.out.printf("Male: %d (%.1f%%)%n", male, percent(male, total));
                System.out.printf("Female: %d (%.1f%%)%n", female, percent(female, total));
                System.out.printf("Non-binary: %d (%.1f%%)%n", nonBinary, percent(nonBinary, total));
                System.out.printf("Other/Unspecified: %d (%.1f%%)%n", otherGender, percent(otherGender, total));
                System.out.printf("Employees with disability: %d (%.1f%%)%n", disabilityCount, percent(disabilityCount, total));
            }

            // Inclusion score metrics from audits
            double sumScore = 0;
            int countScore = 0;
            FindIterable<Document> audits = auditCollection.find();
            for (Document d : audits) {
                Object o = d.get("inclusionScore");
                if (o != null) {
                    try {
                        sumScore += Double.parseDouble(o.toString());
                        countScore++;
                    } catch (Exception ignored) {}
                }
            }
            System.out.println("\n=== Inclusion Score Metrics (from audit records) ===");
            if (countScore == 0) {
                System.out.println("No audit scores available.");
            } else {
                double avg = sumScore / countScore;
                System.out.printf("Average Inclusion Score: %.2f (based on %d records)%n", avg, countScore);
            }

            // Ethnicity breakdown (from employees)
            System.out.println("\n=== Ethnicity Breakdown ===");
            // simple counting by scanning employees and collecting frequencies
            java.util.Map<String, Integer> ethCount = new java.util.HashMap<>();
            cur = head;
            while (cur != null) {
                String e = (cur.ethnicity == null || cur.ethnicity.isBlank()) ? "Unspecified" : cur.ethnicity.trim();
                ethCount.put(e, ethCount.getOrDefault(e, 0) + 1);
                cur = cur.next;
            }
            if (ethCount.isEmpty()) {
                System.out.println("No ethnicity data.");
            } else {
                for (var entry : ethCount.entrySet()) {
                    System.out.printf("%s: %d (%.1f%%)%n", entry.getKey(), entry.getValue(), percent(entry.getValue(), total));
                }
            }
        }

        private double percent(int part, int total) {
            if (total == 0) return 0.0;
            return (100.0 * part) / total;
        }
    }

    // Console menu
    public static void main(String[] args) {
        // MongoDB setup - change URI if needed
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("diversity_audit_db");
        MongoCollection<Document> empCollection = database.getCollection("employees");
        MongoCollection<Document> auditCollection = database.getCollection("audits");

        EmployeeDLL employees = new EmployeeDLL(empCollection, auditCollection);
        employees.loadFromDatabase(); // populate DLL

        Scanner sc = new Scanner(System.in);
        int choice = -1;
        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_DATE;

        menuLoop:
        while (true) {
            System.out.println("\n--- Diversity & Inclusion Audit Tool ---");
            System.out.println("1. Add Employee");
            System.out.println("2. Display Employees");
            System.out.println("3. Search Employee");
            System.out.println("4. Update Employee");
            System.out.println("5. Delete Employee");
            System.out.println("6. Add Audit Record");
            System.out.println("7. Display Audits for Employee");
            System.out.println("8. Compute D&I Metrics");
            System.out.println("9. Exit");
            System.out.print("Enter choice: ");

            if (!sc.hasNextInt()) {
                System.out.print("Invalid input. Enter a number: ");
                sc.next();
                continue;
            }
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter ID: ");
                    String id = sc.nextLine().trim();
                    System.out.print("Enter Name: ");
                    String name = sc.nextLine().trim();
                    System.out.print("Enter Age: ");
                    int age = readIntSafe(sc);
                    System.out.print("Enter Gender (Male/Female/Non-binary/Other): ");
                    String gender = sc.nextLine().trim();
                    System.out.print("Enter Ethnicity: ");
                    String ethnicity = sc.nextLine().trim();
                    System.out.print("Has disability? (yes/no): ");
                    boolean hasDisability = readYesNo(sc);
                    employees.addEmployee(id, name, age, gender, ethnicity, hasDisability);
                }
                case 2 -> employees.displayEmployees();
                case 3 -> {
                    System.out.print("Enter ID to search: ");
                    String id = sc.nextLine().trim();
                    EmployeeNode n = employees.searchEmployee(id);
                    if (n != null) {
                        System.out.printf("Found - ID: %s, Name: %s, Age: %d, Gender: %s, Ethnicity: %s, Disability: %s%n",
                                n.id, n.name, n.age, n.gender, n.ethnicity, n.hasDisability ? "Yes" : "No");
                    } else System.out.println("Employee not found.");
                }
                case 4 -> {
                    System.out.print("Enter ID to update: ");
                    String id = sc.nextLine().trim();
                    EmployeeNode n = employees.searchEmployee(id);
                    if (n == null) { System.out.println("Employee not found."); break; }
                    System.out.print("Enter new name [" + n.name + "]: ");
                    String name = sc.nextLine().trim(); if (name.isEmpty()) name = n.name;
                    System.out.print("Enter new age [" + n.age + "]: ");
                    String ageStr = sc.nextLine().trim(); int age = ageStr.isEmpty() ? n.age : Integer.parseInt(ageStr);
                    System.out.print("Enter new gender [" + n.gender + "]: ");
                    String gender = sc.nextLine().trim(); if (gender.isEmpty()) gender = n.gender;
                    System.out.print("Enter new ethnicity [" + n.ethnicity + "]: ");
                    String ethnicity = sc.nextLine().trim(); if (ethnicity.isEmpty()) ethnicity = n.ethnicity;
                    System.out.print("Has disability? (yes/no) [" + (n.hasDisability ? "yes" : "no") + "]: ");
                    String dis = sc.nextLine().trim(); boolean hasDisability = dis.isEmpty() ? n.hasDisability : dis.equalsIgnoreCase("yes");
                    employees.updateEmployee(id, name, age, gender, ethnicity, hasDisability);
                }
                case 5 -> {
                    System.out.print("Enter ID to delete: ");
                    String id = sc.nextLine().trim();
                    employees.deleteEmployee(id);
                }
                case 6 -> {
                    System.out.print("Enter Employee ID for audit: ");
                    String id = sc.nextLine().trim();
                    System.out.print("Enter Auditor name: ");
                    String auditor = sc.nextLine().trim();
                    System.out.print("Enter date (YYYY-MM-DD) or leave blank for today: ");
                    String dd = sc.nextLine().trim();
                    LocalDate date = dd.isEmpty() ? LocalDate.now() : LocalDate.parse(dd, dateFmt);
                    System.out.print("Enter inclusion score (0-100): ");
                    int score = readIntSafe(sc, 0, 100);
                    System.out.print("Enter notes: ");
                    String notes = sc.nextLine().trim();
                    employees.addAuditRecord(id, auditor, date, score, notes);
                }
                case 7 -> {
                    System.out.print("Enter Employee ID to view audits: ");
                    String id = sc.nextLine().trim();
                    employees.displayAuditsForEmployee(id);
                }
                case 8 -> employees.computeMetrics();
                case 9 -> {
                    System.out.println("Exiting...");
                    break menuLoop;
                }
                default -> System.out.println("Invalid choice.");
            }
        }

        // Close resources
        mongoClient.close();
        sc.close();
    }

    // Helpers
    private static int readIntSafe(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.print("Enter a valid integer: ");
            sc.next();
        }
        int val = sc.nextInt();
        sc.nextLine();
        return val;
    }

    private static int readIntSafe(Scanner sc, int min, int max) {
        int val = readIntSafe(sc);
        while (val < min || val > max) {
            System.out.printf("Enter a value between %d and %d: ", min, max);
            val = readIntSafe(sc);
        }
        return val;
    }

    private static boolean readYesNo(Scanner sc) {
        String resp = sc.nextLine().trim();
        while (!(resp.equalsIgnoreCase("yes") || resp.equalsIgnoreCase("no") || resp.equalsIgnoreCase("y") || resp.equalsIgnoreCase("n"))) {
            System.out.print("Please answer yes or no: ");
            resp = sc.nextLine().trim();
        }
        return resp.equalsIgnoreCase("yes") || resp.equalsIgnoreCase("y");
    }
}