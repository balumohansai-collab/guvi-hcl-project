Inter Departmental Collaboration Hub

A Java–MongoDB based system designed to streamline coordination, information sharing, and collaboration among multiple departments within an institution.

Overview

This application automates inter-departmental communication and monitoring.
It supports:

Adding department collaboration records

Displaying stored records

Searching existing collaborations

Updating collaboration details

Deleting records

Analyzing department collaboration frequency

The system integrates Java with MongoDB and uses a Doubly Linked List for efficient in-memory processing and synchronized storage.

Technologies Used

Java (JDK 8 or higher)

MongoDB Database

MongoDB Java Driver

IntelliJ IDEA / Eclipse / NetBeans

MongoDB Compass (optional)

System Requirements
Hardware Requirements

Minimum: Intel Core i3 with 4 GB RAM

Recommended: Intel Core i5/i7 with 8 GB RAM or more

Software Requirements

Windows / Linux / macOS

Java JDK 8+

MongoDB 4.0 or later

Any IDE supporting Java development

Project Purpose (Aim)

To develop a Java-based platform that supports collaborative communication among institutional departments, maintains shared project data, and produces insights on the frequency and quality of collaboration activities.

How It Works

Connects to MongoDB

Loads existing department collaboration records into a Doubly Linked List

Displays menu-based options for CRUD operations

Updates both database and DLL simultaneously to ensure consistency

Computes collaboration metrics for analytical insights

Exits on user request

Project Structure
src/
└── InterDepartmentalCollaborationHub.java

Features

Add new department collaboration

Display all collaborations

Search collaboration by department or project ID

Update stored collaboration information

Delete existing records

Analyze number of collaborations per department

How to Run
Step 1: Start MongoDB service

Default connection URI:

mongodb://localhost:27017

Step 2: Create database and collections

Database: collaborationHub
Collection: departments

Step 3: Compile and Execute Program
javac InterDepartmentalCollaborationHub.java
java InterDepartmentalCollaborationHub

Output Preview

This project includes:

MongoDB Compass screenshots displaying stored department records

Command-line interface screenshots showing menu operations and actions

(Add your screenshots here)

Conclusion

This project demonstrates:

Automated collaboration management

Java–MongoDB integration

Use of an in-memory data structure (DLL)

Real-time updating of collaboration information

Data-driven insights on department interaction
