# GPU Utilization Management

This is our COSC230 Object Oriented Programming course project. It is a Java Swing application that simulates students sharing four GPUs. Students submit jobs using an allowance of GPU minutes, while an administrator approves accounts, manages allocations and advances the simulation clock.

## Course details

- Khalifa University
- COSC230 Object Oriented Programming — Fall 2026
- Lab section: B2
- Instructor: Mohamed Nagy

## Team members

| Name | Student ID | GitHub username |
| --- | --- | --- |
| Alman Ahmed | 100068766 | Alman-A |
| Ala Ahameed Bin Jaffer | 100068748 | ala-ahmed |

Repository: https://github.com/Alman-A/gpu-utilization

## What the application does

Students can register, sign in after approval, submit jobs, check their balance and view their own job history and waiting-time predictions. They can also search their jobs and cancel queued or running work.

The administrator can approve or reject requests, grant additional minutes, view all jobs, cancel jobs with a reason and monitor GPU utilization.

Jobs use a shared first come first served queue. The four task sizes take 5, 15, 30 and 60 minutes. Accepting a job reserves its full duration. Waiting is free, and cancelling a running job charges only the time it has executed.

## How to run

The project was compiled and tested using JDK 21. To run it in VS Code, install JDK 21 and the Extension Pack for Java.

1. Clone the repository or download and extract its ZIP.
2. In VS Code, use **File > Open Folder** to open the `gpu-utilization` folder containing `src`, `data` and this README.
3. Wait for the Java project to finish loading.
4. Open `src/app/Main.java`.
5. Click **Run** above the `main` method.

Run the application from the project root so it finds `data/simulation.txt`. No external libraries or database are needed.

## Login details

| Account | Username | Password |
| --- | --- | --- |
| Administrator | admin | gpu2026 |
| Ali | S001 | Test123 |
| Sara | S002 | Test123 |
| Hamad | S003 | Test123 |
| Mariam | S004 | Test123 |
| Noura | S005 | Test123 |

Passwords are case-sensitive. The sample data also contains pending request S006 and rejected request S007. Neither has an active student account.

## Sample data and testing

The included `data/simulation.txt` resumes the demonstration at **16/11/2026 10:25**, from a starting time of **10:00**.

It contains five approved students, one pending request, one rejected request and 21 jobs:

| Status | Number of jobs |
| --- | ---: |
| Queued | 8 |
| Running | 4 |
| Completed | 5 |
| Cancelled | 4 |

The final utilization is **97.0%**. J107 is Small, so the report's calculations use its five-minute duration.

Sign in as the administrator to inspect the dashboard, requests, balances and jobs. Sign in as a student to see that student's view. The report explains the test run and includes screenshots of the results.

## Saving and simulation time

Changes are saved automatically to `data/simulation.txt`. Reopening the application restores the saved clock, jobs and balances. Time moves only when the administrator clicks **Advance Minute**.

If saving fails, the application displays an error. Resolve the file-access problem and click **Retry Save** before making further changes.

Keep a backup of the supplied save before continuing the demonstration. To start fresh, close the application and move `simulation.txt` out of the `data` folder. On the next run, sign in as the administrator to set the clock, then register and approve students.

## Source folders

| Folder | Purpose |
| --- | --- |
| `src/app` | Application startup and the separate storage test driver |
| `src/model` | Students, requests, jobs, GPUs and allowance records |
| `src/logic` | Account operations, scheduling and simulation time |
| `src/storage` | Shared application state and text-file saving/loading |
| `src/ui` | Swing windows and dialogs |

`Main.java` starts the application. `StorageTest.java` is a separate persistence test driver.

## Contributions

### Alman Ahmed — Alman-A

- TaskSize.java
- JobStatus.java
- TimeAllowance.java
- GPU.java
- Job.java
- SimulationClock.java
- JobPrediction.java
- Scheduler.java
- ApplicationService.java
- StudentFrame.java
- ClockSetupDialog.java
- Main.java

### Ala Ahameed Bin Jaffer — ala-ahmed

- User.java
- Administrator.java
- Student.java
- AccessRequest.java
- RequestStatus.java
- TimeGrant.java
- AppState.java
- DataStore.java
- FileDataStore.java
- AccountService.java
- LoginFrame.java
- RegistrationDialog.java
- AdminFrame.java

These lists show our main responsibilities. Both members also updated shared classes during integration, as recorded in the commit history.