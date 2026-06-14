# Exam Hall Locator & Real-Time Seating Allocation System

An enterprise-grade, full-stack Exam Operations Management platform designed to replace legacy file-parsing habits with a high-performance relational database engine and real-time administrative reporting. This application automates student seating layouts while maintaining departmental segregation and handling multi-exam scheduling timelines without operational overhead.

---

##  Key Architectural Features (Praxis Framework)

### 1. Asynchronous Real-Time WebSocket Engine
* **Protocol Pipeline:** Implemented a full-duplex messaging layer mapped at `/progressTrack` using the `jakarta.websocket` specification.
* **Non-Blocking  UI Stream:** Connects Java backend worker threads directly to the admin dashboard browser view. When a broadcast is triggered, background threads stream real-time tokens (`PENDING`, `SUCCESS`, `FAILED`) to columns without locking or freezing the administrative view.

### 2. Multi-Exam Session Management
* Shuffled away from restrictive "global" static assignments to a multi-timeline scheduling framework.
* Administrators can provision distinct exam records (Subject, Date, Time, and Target Semester loops), allowing a single student to maintain entirely different, collision-free seat assignments throughout an examination season.

### 3. Algorithmic Seating Allocation Engine
* Implements a custom **Greedy Bin-Packing Optimization Algorithm** to automatically arrange seating charts based on room dimensions.
* **Malpractice Prevention Constraints:** * Enforces a strict maximum of **2 students per bench** (split cleanly into Left and Right node vectors).
    * Evaluates student branch codes (e.g., MCA vs. MBA) during loop extraction to ensure departments are grouped or strategically separated, keeping academic integrity intact.

### 4. Native Multipart Bulk Ingestion
* Uses the Jakarta `@MultipartConfig` framework to eliminate manual database injections.
* Built dynamic, high-performance buffered CSV stream readers that tokenize incoming files character-for-character, utilizing batch executing statements (`executeBatch()`) for optimized data inserts.

### 5. High-Fidelity PDF Ticket Compilation
* Includes a pure localized byte-stream generator that dynamically builds official, printable **PDF Hall Tickets** directly from memory.
* Students validating their USN instantly pull an uncompressed, highly accurate document layout complete with institutional rules and strict verification parameters.

---

## Tech Stack & Dependencies

* **Backend Engine:** Java 17 (Targeted for enterprise class loaders)
* **Web Specifications:** Jakarta Servlet API 6.0.0, Jakarta WebSocket API 2.1.0
* **Database Management:** MySQL 8.3.0 (Hosted natively via XAMPP / phpMyAdmin connection pools)
* **Notification Routing:** Jakarta Mail 2.1.2 & Eclipse Angus Mail Implementation
* **Build Automation Tool:** Apache Maven 3.x
* **User Interfaces:** Dynamic JavaServer Pages (JSP), Native CSS3 UI layouts, JavaScript Event Streams

---

## Database Schema Matrix

The application handles schema initialization gracefully on startup via an automated data-definition block (`DBUtil.ensureSchema()`), which overrides index locks during updates:

```sql
-- 1. Students Registry Table
CREATE TABLE IF NOT EXISTS Students (
    USN VARCHAR(30) PRIMARY KEY,
    Name VARCHAR(120) NOT NULL,
    Email VARCHAR(160),
    Department VARCHAR(80),
    Semester INT DEFAULT 0
);

-- 2. Infrastructure Rooms Table
CREATE TABLE IF NOT EXISTS Rooms (
    RoomID VARCHAR(60) PRIMARY KEY,
    Capacity INT NOT NULL,
    Floor VARCHAR(80)
);

-- 3. Scheduled Examination Sessions Table
CREATE TABLE IF NOT EXISTS Exams (
    ExamID INT AUTO_INCREMENT PRIMARY KEY,
    Subject VARCHAR(160) NOT NULL,
    ExamDate DATE NOT NULL,
    ExamTime VARCHAR(40) NOT NULL,
    Semester INT DEFAULT 0,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Unified Seat Allocations Transaction Matrix
CREATE TABLE IF NOT EXISTS Seat_Allocations (
    AllocationID INT AUTO_INCREMENT PRIMARY KEY,
    ExamID INT NULL,
    USN VARCHAR(30) NOT NULL,
    RoomID VARCHAR(60) NOT NULL,
    BenchNumber INT NOT NULL,
    SeatPosition INT DEFAULT 1,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uniq_allocation_exam_usn (ExamID, USN),
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE
);