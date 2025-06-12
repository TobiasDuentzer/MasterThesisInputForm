# Automated Business Workflow Simulation Based On An Input-Form
This repository holds the code for the input form implementations of my master thesis.

General concept under `MasterThesisInputForm/blob/main/InputForm/src/main/java/InputFormPackage/InputForm.java`

BIMP specific implementation under `MasterThesisInputForm/blob/main/InputForm/src/main/java/InputFormPackage/InputFormBIMP.java`

This project provides a dynamic JavaFX-based input form to collect business workflow and simulation information . It is designed to streamline the creation and editing of BPMN-compatible simulation models with a user-friendly graphical interface. The BIMP specific version ist tailored to the BIMP online simulator [https://bimp.cs.ut.ee/simulator/]. This version when filled in correctly, produces a .bpmn file which can directly be imported into BIMP and consequently simulated. False entry can result in a file which is unable to be imported!

## Features

- Dynamic form generation for:
  - Workflow definition
  - Resources allocation
  - Simulation settings
- Input validation with restricted inputs to ensure BPMN compatibility
- User guidance via tooltips and highlighting of errors
- Export and save configurations to a specified directory
- Load existing `.CSV` files for editing (file name must include form column name e.g. "simulation" to load the selected file into the simulation section of the form.)

## Technologies Used

- Java 11+
- JavaFX
- Camunda BPMN Model API
- XML DOM API

## Project applications
Main JavaFX applications are InputForm.java for the general input form and InputFormBIMP.java for the BIMP specific one.

## Requirements
- Java 11 or higher
- JavaFX SDK
- Camunda BPMN Model API

## How to Run
1. Clone or download the repository.
2. Open the project in your IDE (IntelliJ IDEA, Eclipse, etc.).
3. Ensure JavaFX SDK is configured in your project.
4. Build and run `InputFormBIMP.java`.
5. Fill in the form and click "Create BPMN".
6. Save the generated `.bpmn` file.
7. Import the file into (https://bimp.cs.ut.ee/simulator/).
8. Start your simulation.
