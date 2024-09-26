# CS 6378: Advanced Operating Systems  
**Project 1**  
**Instructor:** Prof. Neeraj Mittal  
**Author:** Malay Virendra Bhavsar (MXB230055)

---

## Project Overview

This individual project implements a distributed system using the Chandy-Lamport snapshot-taking protocol and the MAP (Message Active Passive) protocol. Developed in **Java (Version 22.0.2)**, the project emphasizes socket programming and distributed communication, running exclusively on the `dcXX.utdallas.edu` machines.

---

## Project Components

The project comprises several essential components:

- **Node**: Sets up Client, Server, and ChandyLamport instances; manages the configuration file.
  
- **Server**: Oversees incoming communications and directs them appropriately.
  
- **Client**: Manages outgoing communications, including application and control messages.
  
- **ChandyLamport**: Implements the snapshot protocol and ensures the consistency of snapshots.
  
- **Message**: Establishes a structured format for inter-process message exchange.

---

## Project Segments

The project is divided into four main segments:

1. **Part 1**: Implement a distributed system with `n` nodes, each able to send messages according to the MAP protocol. Nodes transition between active and passive states based on message counts and delays.

2. **Part 2**: Integrate Chandy and Lamport’s protocol to record a consistent global snapshot, initiated by node 0, to detect the termination of the MAP protocol.

3. **Part 3**: Implement Fidge/Mattern’s vector clock protocol to verify the consistency of snapshots through vector timestamps.

4. **Part 4**: Create a protocol for halting all nodes after node 0 detects MAP protocol termination.

---

## Configuration Format

The project utilizes a plain-text configuration file formatted as follows:

- The first line contains six tokens: 
    - `Number of nodes`
    - `minPerActive`
    - `maxPerActive`
    - `minSendDelay`
    - `snapshotDelay`
    - `maxNumber`.
- The next `n` lines specify node details:
    - `nodeId`
    - `hostname`
    - `port`
- The subsequent `n` lines list neighboring nodes.


---

## Output Format

For a configuration file named `<config_name>.txt` with `n` nodes, the program will generate `n` output files named `<config_name>-<node_id>.out`. Each file contains vector timestamps for each snapshot recorded by the respective process.

**Example Output:**
```
0 4 3 6 0 2 3
3 7 6 7 2 4 4
6 9 11 10 5 7 5
8 12 14 23 8 10 7
```
---

## Getting Started

### Prerequisites

- Ensure you have **Java Development Kit (JDK) 22.0.2** installed.
- This project must be executed on the machines `dcXX.utdallas.edu` (where XX ∈ {01, 02, ..., 45}).

### Setup Instructions

1. **Create a project directory:**
   ```bash
   mkdir aos-project1
   cd aos-project1
   ```
   
2. Place all project files (including the configuration file) in this directory.

3. Compile the project:
  ```bash
  javac *.java
  ```

4. Clean up before and after running the program:
  ```bash
  chmod +x cleanup.sh
  ./cleanup.sh
  ```

5. Run the program:
  ```bash
  chmod +x launcher.sh
  ./launcher.sh
  ```

6. Perform cleanup after use:
  ```bash
  ./cleanup.sh
  ```

### Connecting to the Server

To connect to the server at `dcXX.utdallas.edu`, use the following command (omit the password):
```bash
ssh <your-username>@dcXX.utdallas.edu
```
Ensure that you replace <your-username> with your actual username. After connecting, you can navigate to your project directory to compile and run your code.

---

## Acknowledgments

The `./launcher.sh` and `./cleanup.sh` scripts are provided by the professor. All credits go to the respective owners for these contributions.

---

## Conclusion

This project delves into the intricacies of distributed systems, showcasing effective communication and state consistency through the Chandy-Lamport protocol. We encourage you to explore these concepts deeply and enhance your understanding.

If you have any questions or require further assistance, please don't hesitate to reach out!

---


