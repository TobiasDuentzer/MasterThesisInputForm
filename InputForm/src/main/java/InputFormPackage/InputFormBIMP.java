package InputFormPackage;

import javafx.application.Application;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Integer.valueOf;

public class InputFormBIMP extends Application {
    // VBox containers for organizing the UI into three vertical sections:
    private VBox colWf;
    private VBox colRes;
    private VBox colSim;
    // Counter to assign unique IDs to newly created tasks
    private Integer taskID = 1;
    // List to keep track of all defined resource names (used for validation in tasks)
    private List<String> resourceNames = new ArrayList<>();
    // List to keep track of all defined timetables names (used for validation in resources)
    private List<String> timetableNames = new ArrayList<>();
    // Directory where the current input form data will be saved
    private File saveDirectory = null;
    // ChangeListener to enforce numeric-only input in TextFields
    // Reverts the input to the previous value if the new value contains non-digit characters
    private final ChangeListener<String> numeric = (observable, oldValue, newValue) -> {
        if (newValue.matches("\\d*")) return;
        ((StringProperty) observable).set(oldValue);
    };
    private final ChangeListener<String> numericWithDecimal = (observable, oldValue, newValue) -> {
        if (newValue.matches("\\d*(\\.\\d*)?")) return;
        ((StringProperty) observable).set(oldValue);
    };
    // Dummy ChangeListener that performs no action — used when no listener is desired
    private final ChangeListener<String> noCommaAndBraces = (observable, oldValue, newValue) -> {
        // Remove unwanted characters: commas, brackets, braces
        String cleaned = newValue.replaceAll("[{},,]", "");
        // If cleaned differs from newValue, update the text
        if (!cleaned.equals(newValue)) {
            ((StringProperty) observable).set(cleaned);
            return;
        }
    };


    @Override
    public void start(Stage primaryStage) {
        // Get the screen dimensions
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        //----------------------Workflow----------------------
        // Initialize the workflow column VBox with title "Tasks"
        colWf = colSetup("Tasks");

        // Add the first task group to the column (empty values by default)
        VBox firstTextFieldGroup = createTaskGroup(colWf, noCommaAndBraces, taskID,"", "", List.of(), Arrays.asList("", "", ""), Arrays.asList("", "", "", ""));
        colWf.getChildren().add(firstTextFieldGroup);
        taskID++;

        // Button to dynamically add new task groups
        Button addGroupButton = new Button("Add New Task");
        addGroupButton.setOnAction(event -> {
            // Create a new task group with empty initial values
            VBox newTaskGroup = createTaskGroup(colWf, noCommaAndBraces, taskID,"", "", List.of(), Arrays.asList("", "", ""), Arrays.asList("", "", "", ""));
            // Insert it before the button itself
            colWf.getChildren().add(colWf.getChildren().size() - 1, newTaskGroup);
            taskID++;
        });

        // Add the task-adding button to the end of the workflow column
        colWf.getChildren().add(addGroupButton);


        //----------------------Resources and Timetables----------------------
        // Initialize the resource column VBox with title "Resources and timetables"
        colRes = colSetup("Resources and Timetables");
        colRes.setMaxWidth(550);
        colRes.setPrefWidth(550);

        // Container for resource fields
        VBox descriptionsHBox = new VBox(10);
        descriptionsHBox.setAlignment(Pos.TOP_LEFT);
        descriptionsHBox.setStyle("-fx-border-color: gray; -fx-border-width: 2px; -fx-padding: 5px;");
        descriptionsHBox.setMaxWidth(550);   // Limit the width to keep alignment clean

        Label reslabel = new Label("Resources:");
        reslabel.setStyle("-fx-font-size: 12px;");

        // Create the first editable resource input row with default values
        HBox firstTextFieldContainer = createResourceField(descriptionsHBox, "", "", "", "");

        // Button to dynamically add more resource input rows
        Button addResButton = new Button("+");
        addResButton.setOnAction(event -> {
            HBox newTextFieldContainer = createResourceField(descriptionsHBox, "", "", "", "");
            // Add new row just before the button
            descriptionsHBox.getChildren().add(descriptionsHBox.getChildren().size()-1, newTextFieldContainer);
        });

        // Add headers, first input row, and add-button to the resource column
        descriptionsHBox.getChildren().addAll(firstTextFieldContainer, addResButton);

        // Container for resource field headers (aligned horizontally)
        VBox descriptionsHBox2 = new VBox(10);
        descriptionsHBox2.setAlignment(Pos.TOP_LEFT);
        descriptionsHBox2.setStyle("-fx-border-color: gray; -fx-border-width: 2px; -fx-padding: 5px;");
        descriptionsHBox2.setMaxWidth(550);   // Limit the width to keep alignment clean

        Label ttlabel = new Label("Timetables:");
        ttlabel.setStyle("-fx-font-size: 12px;");

        // Create the first editable timetable input row with default values
        HBox firstTimetableContainer = createTimetableField(descriptionsHBox2, "", "", "", "", "");

        // Button to dynamically add more resource input rows
        Button addTimetableButton = new Button("+");
        addTimetableButton.setOnAction(event -> {
            HBox newTextFieldContainer = createTimetableField(descriptionsHBox2, "", "", "", "", "");
            // Add new row just before the button
            descriptionsHBox2.getChildren().add(descriptionsHBox2.getChildren().size()-1, newTextFieldContainer);
        });

        // Add headers, first input row, and add-button to the resource column
        descriptionsHBox2.getChildren().addAll(firstTimetableContainer, addTimetableButton);
        colRes.getChildren().addAll(reslabel, descriptionsHBox, ttlabel, descriptionsHBox2);


        //----------------------Simulation----------------------
        // Initialize the simulation column VBox with title "Simulation"
        colSim = colSetup("Simulation");

        // Add simulation parameter input fields (using restriction to numeric input)
        ObservableList<String> optionsDis =
                FXCollections.observableArrayList(
                        "Fixed",
                        "Normal",
                        "Exponential",
                        "Uniform",
                        "Triangular",
                        "Log-Normal",
                        "Gamma"
                );
        final ComboBox comboBoxDis = createComboBox(optionsDis,new Tooltip("Distribution of arrival time"),130.0,"Inter arrival time");
        Label cblabelDis = new Label("Inter arrival time");
        cblabelDis.setStyle("-fx-font-size: 12px;");

        TextField durTextField = new TextField();
        durTextField.setPromptText("");
        durTextField.setMaxWidth(120);

        // Add listener to handle selection changes
        comboBoxDis.setOnAction(event -> {
            String selected = (String) comboBoxDis.getSelectionModel().getSelectedItem();
            // Change string depending on selected item
            String result;
            if (selected.equals("Fixed")) {
                result = "to";
            } else if (selected.equals("Normal")) {
                result = "Mean, Std deviation";
            } else if (selected.equals("Exponential")) {
                result = "Mean";
            } else if (selected.equals("Uniform")) {
                result = "between, and";
            } else if (selected.equals("Triangular")) {
                result = "Mode, Minimum, Maximum";
            } else if (selected.equals("Log-Normal")) {
                result = "Mean, Variance";
            } else if (selected.equals("Gamma")) {
                result = "Mean, Variance";
            } else {
                result = "";
            }

            // Update label or perform other actions
            durTextField.setPromptText(result);
        });

        ObservableList<String> optionsUnit =
                FXCollections.observableArrayList(
                        "Seconds",
                        "Minutes",
                        "Hours",
                        "Days"
                );
        final ComboBox comboBoxUnit = createComboBox(optionsUnit,new Tooltip("Time unit for the values"),80.0,"Unit");
        Label cblabelUnit = new Label("Distribution");
        cblabelUnit.setStyle("-fx-font-size: 12px;");

        HBox atHBox = new HBox(10);
        atHBox.getChildren().addAll(comboBoxDis, durTextField, comboBoxUnit);

        TextField startTextField = createTextField(new Tooltip("Specifiy the simulation start month day and time in UTC(e.g. '2025-06-01 09:00')."),120.0,"Year-Month-Day HH:MM");
        Label startlabel = new Label("Scenario start date and time");
        startlabel.setStyle("-fx-font-size: 12px;");
        startTextField.setPrefWidth(120);

        HBox piHBox = new HBox(15);
        Label piLabel = new Label("Total number of process instances.       % to exclude form stats");
        // Total number of entities to arrive during the simulation
        String tooltipString = "Set the total number of process instances to be created during the simulation (e.g., '100' means the simulation stops after 100 arrivals).";
        createTextfieldHBox(piHBox, "", "Total Arrival Count", "", 200, numeric, tooltipString);
        tooltipString = "Value must be between 0 and 40. Specifies the percentage of process instance performance stats to exclude from the start and the end of the simulation scenario. Use to exclude statistics from process inctances when scenario is 'warming up' or 'cooling down'.";
        createTextfieldHBox(piHBox, "", "%start", "", 70, numeric, tooltipString);
        createTextfieldHBox(piHBox, "", "%end", "", 70, numeric, tooltipString);

        // Input for currency (e.g., euro,usd...);
        ObservableList<String> options =
                FXCollections.observableArrayList(
                        "EUR",
                        "USD",
                        "CAD",
                        "GBP",
                        "CHF",
                        "NZD",
                        "AUD",
                        "JPY"
                );
        final ComboBox comboBoxCurr = createComboBox(options,new Tooltip("Currency is information only. Used in the simulation results page."),90.0,"Currency");
        Label currlabel = new Label("Currency");
        currlabel.setStyle("-fx-font-size: 12px;");

        colSim.getChildren().addAll(atHBox, piLabel, piHBox, startlabel, startTextField, currlabel, comboBoxCurr);

        //----------------------Save----------------------
        // VBox to contain save/load UI elements
        VBox colSave = new VBox(10);
        colSave.setAlignment(Pos.TOP_LEFT);
        colSave.setStyle("-fx-padding: 10;");
        colSave.setPrefWidth(350);  // Preferred width for consistency
        colSave.setMaxWidth(400);   // Limit maximum width

        // Create Save button and a label to display selected directory
        Button saveButton = new Button("Save Inputs");
        Label labelSaveB = new Label("no directory selected");
        VBox saveFileBox = new VBox();

        // Save button logic
        saveButton.setOnAction(event -> {
            if (saveDirectory == null) {
                // Prompt user to select a directory
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choose Save Location");
                directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                saveDirectory = directoryChooser.showDialog(saveButton.getScene().getWindow());
            }
            if(saveDirectory!=null){
                // Show selected path
                labelSaveB.setText(saveDirectory.getAbsolutePath());
            }

            // Collect and save input data to CSV files
            List<bimpTimetable> timetables = collectTimetableData();
            saveTimetablesToCSV(timetables);

            List<bimpAvailableResource> resources = collectResourcesData(timetables);
            saveResourcesToCSV(resources);

            List<bimpTask> tasks = collectTaskData();
            saveTaskToCSV(tasks);

            bimpSimulation sim = collectSimulationData();
            saveSimulationToCSV(sim);
        });

        // Add Save UI components to VBox
        saveFileBox.getChildren().addAll(saveButton, labelSaveB);

        //----------------------Load Section----------------------

        // File chooser for loading input from file
        FileChooser fil_chooser = new FileChooser();
        Label labelLoadB = new Label("");
        Button button = new Button("Load from File");
        VBox loadFileBox = new VBox();

        // Event handler to trigger file loading
        EventHandler<ActionEvent> event = e -> {
            File file = fil_chooser.showOpenDialog(primaryStage);
            if(file != null){
                loadEvent(file); // Custom method to handle loading
            }
        };

        button.setOnAction(event);
        // Add Load UI components to VBox
        loadFileBox.getChildren().addAll(button, labelLoadB);

        //----------------------Reset Button----------------------

        // Button to clear all inputs and restart application
        Button restartButton = new Button("Clear All");
        restartButton.setOnAction(e -> restartApplication(primaryStage));

        //----------------------BPMN Button----------------------

        // Button to clear all inputs and restart application
        Button bpmnButton = new Button("Create BPMN");
        bpmnButton.setOnAction(e -> {
            List<bimpTimetable> timetables = collectTimetableData();
            List<bimpAvailableResource> resources = collectResourcesData(timetables);
            List<bimpTask> tasks = collectTaskData();
            bimpSimulation sim = collectSimulationData();
            createBPMN(tasks, saveConnectionsToList(tasks), sim, timetables, resources);
        });

        //----------------------Top UI Bar----------------------

        // Spacer to offset controls slightly from the left
        Region spacerSave = new Region();
        spacerSave.setMaxWidth(40);

        // Horizontal bar with load, save, and reset buttons
        HBox top = new HBox(40);
        top.setAlignment(Pos.TOP_LEFT);
        top.getChildren().addAll(spacerSave, loadFileBox, saveFileBox, bpmnButton, restartButton);

        //----------------------Scrollable Content Sections----------------------

        // Make each major column scrollable
        ScrollPane sPWf = new ScrollPane(colWf);
        sPWf.setFitToWidth(true);
        ScrollPane sPRes = new ScrollPane(colRes);
        sPRes.setFitToWidth(true);
        ScrollPane sPSim = new ScrollPane(colSim);
        sPSim.setFitToWidth(true);

        //----------------------Layout Composition----------------------

        // Top-level layout

        HBox toptop = new HBox(20);  // Empty placeholder for future top-level controls
        HBox bottom = new HBox(20, sPWf, sPRes, sPSim, colSave);  // Horizontal layout for form sections
        VBox root = new VBox(20, toptop, top, bottom);  // Root layout with vertical stacking

        root.setAlignment(Pos.TOP_LEFT);

        // Set up the scene and show it
        Scene scene = new Scene(root, screenWidth, screenHeight);
        primaryStage.setTitle("InputForm");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
    /**
     * Restarts the application by closing the current primary stage
     * and launching a new instance of InputForm.
     *
     * @param primaryStage The current stage to be closed and replaced.
     */
    private void restartApplication(Stage primaryStage) {
        try {
            // Close the current application window
            primaryStage.close();

            // Create a new instance of the application and launch it
            InputFormBIMP mainApp = new InputFormBIMP();
            Stage newStage = new Stage();
            mainApp.start(newStage);  // Start a fresh GUI window
        } catch (Exception ex) {
            // Log any unexpected errors
            ex.printStackTrace();
        }
    }
    /**
     * Creates a styled VBox column with a given section title.
     * Used for initializing the layout of the main input sections.
     *
     * @param titleName The title to be displayed at the top of the column.
     * @return Configured VBox with title label.
     */
    private VBox colSetup(String titleName){
        VBox col = new VBox(10);  // Create vertical layout with spacing
        col.setAlignment(Pos.TOP_LEFT);
        col.setStyle("-fx-padding: 10");  // Add internal padding
        col.setPrefWidth(450);            // Set preferred width
        col.setMaxWidth(450);             // Limit maximum width

        // Create and style the section title
        Label title = new Label(titleName);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        col.getChildren().add(title);  // Add title to column

        return col;
    }
    /**
     * Creates a VBox group for a task with fields for task name, required resources,
     * following tasks, and duration. Includes functionality to add and remove entries dynamically.
     *
     * @param parent   The parent VBox to which this group belongs.
     * @param taskID   The unique identifier of the task.
     * @param name     The name of the task.
     * @param res      Array of required resources in the format "resourceName:amount".
     * @param fTasks   Array of following tasks in the format "taskName:{requirement}".
     * @param dur      Array containing duration values: [hours, minutes, seconds].
     * @return         The constructed VBox representing the task input group.
     */
    private VBox createTaskGroup(VBox parent, ChangeListener x, Integer taskID, String name, String res, List<String> fTasks, List<String> dur, List<String> cost) {
        VBox textFieldGroupBox = new VBox(10);
        VBox textFieldGroup = new VBox(10);
        textFieldGroup.setAlignment(Pos.TOP_LEFT);
        textFieldGroup.setStyle("-fx-border-color: gray; -fx-border-width: 2px; -fx-padding: 5px;");

        // Task name input
        TextField firstTextField = createTextField(new Tooltip("Enter a short, descriptive name for the task (e.g., 'Review Application' or 'Approve Invoice'). This name will help identify the task in the process flow."),550.0,"Task Name");
        if(!name.equals("")&&!name.equals("null")) firstTextField.setText(name);

        // ------------------ Required Resources Section ------------------
        TextField resTextField = createTextField(new Tooltip("Specify the name of the resource needed to perform this task (e.g., 'Employee', 'Machine A'). Make sure it is listed in the Resource column!"),550.0,"Resource Name");
        if(!res.equals("")&&!res.equals("null")) resTextField.setText(res);


        // ------------------ Following Tasks Section ------------------
        Label followingTasksLabel = new Label("Following Tasks");
        followingTasksLabel.setStyle("-fx-font-size: 12px;");
        VBox followingTaskLinesContainer = new VBox(10);

        String firstFollowingTaskLineName = "";
        String firstFollowingTaskLineReq = "";

        // Load existing following task lines
        if(fTasks.size()>0) {
            firstFollowingTaskLineName = fTasks.get(0).substring(1,fTasks.get(0).length()-1).split(",")[0];
            firstFollowingTaskLineReq = fTasks.get(0).substring(1,fTasks.get(0).length()-1).split(",")[1];
        }
        HBox firstFollowingTaskLine = createNextTaskLine(noCommaAndBraces, followingTaskLinesContainer,firstFollowingTaskLineName,firstFollowingTaskLineReq);
        followingTaskLinesContainer.getChildren().add(firstFollowingTaskLine);

        for (int i = 1; i < fTasks.size(); i++) {
            if(!fTasks.get(i).equals("{null,null}")) {
                String reqWOBrackets = fTasks.get(i).substring(1,fTasks.get(i).length()-1).split(",")[1];
                HBox newFollowingTaskLine = createNextTaskLine(noCommaAndBraces, followingTaskLinesContainer, fTasks.get(i).substring(1,fTasks.get(i).length()-1).split(",")[0], reqWOBrackets);
                followingTaskLinesContainer.getChildren().add(newFollowingTaskLine);
            }
        }

        // Button to add more following task fields
        Button addLineButtonTwo = new Button("+");
        addLineButtonTwo.setOnAction(event -> {
            HBox newFollowingTaskLine = createNextTaskLine(noCommaAndBraces, followingTaskLinesContainer,"","");
            followingTaskLinesContainer.getChildren().add(newFollowingTaskLine);  // Add new line
        });

        // Delete button for removing the task group
        Button deleteButton = new Button("Delete Group");
        deleteButton.setOnAction(event -> {
            parent.getChildren().remove(textFieldGroup);
            parent.getChildren().remove(textFieldGroupBox);
        });

        HBox deleteButtonContainer = new HBox();
        deleteButtonContainer.getChildren().add(deleteButton);
        deleteButtonContainer.setAlignment(Pos.BOTTOM_RIGHT); // Align the button to the right side of the HBox

        // ------------------ Duration Section ------------------
        ObservableList<String> optionsDis =
                FXCollections.observableArrayList(
                        "Fixed",
                        "Normal",
                        "Exponential",
                        "Uniform",
                        "Triangular",
                        "Log-Normal",
                        "Gamma"
                );
        final ComboBox comboBoxDis = createComboBox(optionsDis,new Tooltip("Distribution of duration"),110.0,"Distribution");
        Label cblabelDis = new Label("Distribution");
        cblabelDis.setStyle("-fx-font-size: 12px;");

        if (dur.get(0) != null && !dur.get(0).equals("null") && !dur.get(0).equals("")) comboBoxDis.setValue(dur.get(0));

        Label durationLabel = new Label("Duration");
        durationLabel.setStyle("-fx-font-size: 12px;");
        HBox timeHBox = new HBox(10);
        ObservableList<String> optionsUnit =
                FXCollections.observableArrayList(
                        "Seconds",
                        "Minutes",
                        "Hours",
                        "Days"
                );
        final ComboBox comboBoxUnit = createComboBox(optionsUnit,new Tooltip("Time unit for the values"),80.0,"Unit");
        Label cblabelUnit = new Label("Distribution");
        cblabelUnit.setStyle("-fx-font-size: 12px;");
        if (dur.get(2) != null && !dur.get(2).equals("null") && !dur.get(2).equals("")) comboBoxUnit.setValue(dur.get(2));

        TextField durTextField = new TextField();
        durTextField.setPromptText("");
        durTextField.setPrefWidth(170);
        if (dur.get(1) != null && !dur.get(1).equals("null") && !dur.get(1).equals("")) durTextField.setText(dur.get(1));

        // Add listener to handle selection changes
        comboBoxDis.setOnAction(event -> {
            String selected = (String) comboBoxDis.getSelectionModel().getSelectedItem();
            // Change string depending on selected item
            String result;
            if (selected.equals("Fixed")) {
                result = "to";
            } else if (selected.equals("Normal")) {
                result = "Mean, Std deviation";
            } else if (selected.equals("Exponential")) {
                result = "Mean";
            } else if (selected.equals("Uniform")) {
                result = "between, and";
            } else if (selected.equals("Triangular")) {
                result = "Mode, Minimum, Maximum";
            } else if (selected.equals("Log-Normal")) {
                result = "Mean, Variance";
            } else if (selected.equals("Gamma")) {
                result = "Mean, Variance";
            } else {
                result = "";
            }

            // Update label or perform other actions
            durTextField.setPromptText(result);
        });

        timeHBox.getChildren().addAll(comboBoxDis, durTextField, comboBoxUnit);

        // ------------------ Cost Section ------------------
        HBox costHBox = new HBox(10);
        createTextfieldHBox(costHBox, cost.get(0), "Fixed cost", "", 90, numeric, "Fixed cost of Task. For decimal point should be used.");
        createTextfieldHBox(costHBox, cost.get(1), "Cost threshold", "", 90, numeric, "If set, then additional statistics will be generated for the tasks exceeding threshold value. Includes fixed cost. For decimal place, point should be used.");
        createTextfieldHBox(costHBox, cost.get(2), "Duration threshold", "", 90, numeric, "If set, then additional statistics will be generated for the tasks exceeding threshold value. For decimal place, point should be used.");
        ObservableList<String> optionsCost =
                FXCollections.observableArrayList(
                        "Seconds",
                        "Minutes",
                        "Hours",
                        "Days"
                );
        final ComboBox comboBoxCost = createComboBox(optionsCost, new Tooltip("Choose how the simulation should run: 'Virtual Time' runs as fast as possible, 'Real Time' follows actual clock time."),80.0, "Unit");
        Label cblabelCost = new Label("Distribution");
        cblabelCost.setStyle("-fx-font-size: 12px;");
        costHBox.getChildren().add(comboBoxCost);
        if (cost.get(3) != null && !cost.get(3).equals("null") && !cost.get(3).equals("")) comboBoxCost.setValue(cost.get(3));

        Label costLabel = new Label("Fixed cost and thresholds");
        costLabel.setStyle("-fx-font-size: 12px;");

        // ------------------ Header Section ------------------
        HBox header = new HBox(10);
        Label taskLabel = new Label("Task ID: " + taskID);
        taskLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        // Show/Hide toggle for the group
        Button hideButton = new Button("Hide");
        hideButton.setOnAction(event -> {
            if(hideButton.getText().equals("Hide")){
                hideButton.setText("Show");
                textFieldGroup.setVisible(false);
                textFieldGroup.setManaged(false);
            }
            else {
                hideButton.setText("Hide");
                textFieldGroup.setVisible(true);
                textFieldGroup.setManaged(true);
            }
        });

        Region spacer = new Region();
        spacer.setMinWidth(100);
        HBox.setHgrow(spacer, Priority.ALWAYS); // Allow the spacer to take up remaining space

        header.getChildren().addAll(taskLabel, spacer, hideButton);

        // Add all components to the group
        textFieldGroup.getChildren().addAll(firstTextField, resTextField,
                followingTasksLabel, followingTaskLinesContainer, addLineButtonTwo,
                durationLabel, timeHBox, costLabel, costHBox, deleteButtonContainer);

        // Combine header and group
        textFieldGroupBox.getChildren().addAll(header, textFieldGroup);

        return textFieldGroupBox;
    }

    /**
     * Creates combobox and adds properties
     *
     * @param list the list of options
     * @param tt the tooltip
     * @param width the width of the box
     * @param prompt the prompt
     */
    private ComboBox createComboBox(ObservableList<String> list, Tooltip tt, Double width, String prompt) {
        final ComboBox cb = new ComboBox(list);
        tt.setStyle("-fx-font-size: 14px;");
        cb.setTooltip(tt);
        cb.setPrefWidth(width);
        cb.setPromptText(prompt);
        return cb;
    }
    /**
     * Creates a new textfield and adds properties
     *
     * @param tt the tooltip
     * @param width the width of the field
     * @param prompt the prompt
     */
    private TextField createTextField(Tooltip tt, Double width, String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(width);
        tt.setStyle("-fx-font-size: 14px;");
        tf.setTooltip(tt);
        return tf;
    }

    /**
     * Creates following task text field line
     *
     * @param parent the container VBox to which the next task line is added
     * @param nextID the ID of the following task
     * @param req the requirement/condition for the following task
     */
    private HBox createNextTaskLine(ChangeListener x, VBox parent, String nextID, String req) {
        HBox line = new HBox(10);  // Horizontal box with 10px spacing

        // TextField for task ID (numeric input only)
        TextField mainTextField = createTextField(new Tooltip("List the ID(s) of the task(s) that come immediately after this one (e.g., '1', '2'). It is found above the Task Name."),60.0,"Task ID");
        if(!nextID.equals("")&&!nextID.equals("null")) mainTextField.setText(nextID);
        mainTextField.textProperty().addListener(numeric);

        // TextField for the requirement description
        TextField requirements = createTextField(new Tooltip("In percentage only! All conditions have to sum up to 100% (e.g. 0.1 = 10%). If one following task has a condition all other have to have one too. Empty = Parallel, Condition = Exclusive"),100.0,"% Condition");
        requirements.textProperty().addListener(numericWithDecimal);
        if(!req.equals("")&&!req.equals("null")) requirements.setText(req);

        // Delete button to remove this line
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            parent.getChildren().remove(line);
        });

        // Assemble the line
        line.getChildren().addAll(mainTextField, requirements, deleteButton);
        return line;
    }

    /**
     * Creates a labeled input field for specifying an available resource.
     * Adds the field, amount input, cost per hour, timetalbe, and delete button to the given HBox parent.
     *
     * @param parent the container HBox to which the fields are added
     * @param c1 the pre-filled resource name
     * @param c2 the pre-filled amount
     * @param c3 the pre-filled cost per hour
     * @param c4 the pre-filled timetalbe
     */
    private HBox createResourceField(Pane parent, String c1, String c2, String c3, String c4) {
        HBox textFieldContainer = new HBox(10);  // 10px spacing
        textFieldContainer.setAlignment(Pos.BOTTOM_LEFT);  // Align children to bottom left

        // TextField for the resource name
        TextField firstTextField = createTextField(new Tooltip("Enter the name of the resource that can be used in tasks (e.g., 'Employee', 'Forklift', 'Approval Software'). Use consistent names across all tasks."),130.0,"Name");
        firstTextField.setText(c1);

        // TextField for the amount
        TextField secoundTextField = createTextField(new Tooltip("Specify how many units of this resource are available in total for the process (e.g., '3' employees, '2' forklifts)."),100.0,"# of Resources");
        secoundTextField.textProperty().addListener(numeric);
        secoundTextField.setText(c2);

        // TextField for the cost per hour
        TextField thirdTextField = createTextField(new Tooltip("Specify the cost per hour for this resource."),90.0,"Cost per Hour");
        thirdTextField.textProperty().addListener(numeric);
        thirdTextField.setText(c3);

        // TextField for the timetable
        TextField fourthTextField = createTextField(new Tooltip("Enter the name of the timetable for this resource."),90.0,"Timetable");
        fourthTextField.setText(c4);

        Region spacerSave = new Region();
        spacerSave.setPrefWidth(10);


        // Delete button for removing the row
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            parent.getChildren().remove(textFieldContainer);
        });

        // Assemble the line
        textFieldContainer.getChildren().addAll(firstTextField, secoundTextField, thirdTextField, fourthTextField, spacerSave, deleteButton);
        return textFieldContainer;
    }

    /**
     * Creates a labeled input field for specifying an available resource.
     * Adds the field, amount input, cost per hour, timetalbe, and delete button to the given HBox parent.
     *
     * @param parent the container HBox to which the fields are added
     * @param name the pre-filled resource name
     * @param sd the pre-filled stat day
     * @param ed the pre-filled end day
     * @param st the pre-filled start time
     * @param et the pre-filled end time
     */
    private HBox createTimetableField(VBox parent, String name, String sd, String ed, String st, String et) {
        HBox textFieldContainer = new HBox(7);  // 10px spacing
        textFieldContainer.setAlignment(Pos.BOTTOM_LEFT);  // Align children to bottom left

        // TextField for the timetable name
        TextField firstTextField = createTextField(new Tooltip("Enter the name of the timetable. Must contain at least 1 letter!"),130.0,"Name");
        if (!name.equals("null"))firstTextField.setText(name);


        // combobox for the start day
        ObservableList<String> optionsBday =
                FXCollections.observableArrayList(
                        "Monday",
                        "Tuesday",
                        "Wednesday",
                        "Thursday",
                        "Friday",
                        "Saturday",
                        "Sunday"
                );
        final ComboBox comboBoxBday = createComboBox(optionsBday,new Tooltip("Specify the first day of the week when the resource is available."),90.0,"Begin day");
        if (!sd.equals("null") && !sd.equals(""))comboBoxBday.setValue(sd);

        // combobox for the end day
        final ComboBox comboBoxEday = createComboBox(optionsBday,new Tooltip("Specify the last day of the week when the resource is available."),90.0,"End day");
        if (!ed.equals("null") && !ed.equals(""))comboBoxEday.setValue(ed);

        // TextField for the begin time
        TextField fourthTextField = createTextField(new Tooltip("Time of day when resource becomes available HH:MM."),60.0,"HH:MM");
        if (!st.equals("null"))fourthTextField.setText(st);

        // TextField for the end time
        TextField fifthTextField = createTextField(new Tooltip("Time of day when resource becomes unavailable HH:MM."),60.0,"HH:MM");
        if (!et.equals("null"))fifthTextField.setText(et);

        // Delete button for removing the row
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            parent.getChildren().remove(textFieldContainer);
        });

        // Assemble the line
        textFieldContainer.getChildren().addAll(firstTextField, comboBoxBday, comboBoxEday, fourthTextField, fifthTextField, deleteButton);
        return textFieldContainer;
    }

    /**
     * Creates a labeled TextField with a prompt, content, and a change listener.
     * The label is added above the TextField.
     *
     * @param parent the container HBox to which the TextField and label are added
     * @param content the initial content of the TextField
     * @param prompt the placeholder text
     * @param label the label text shown above the TextField
     * @param width the maximum width of the TextField
     * @param L the ChangeListener to observe text changes
     */
    private void createTextfieldHBox(HBox parent, String content, String prompt, String label, double width, ChangeListener L, String tooltipString){
        TextField mainTextField = createTextField(new Tooltip(tooltipString),width,prompt);
        parent.getChildren().add(mainTextField);            // Add TextField to the given HBox
        mainTextField.textProperty().addListener(L);        // Attach listener (e.g., to enforce numeric input)

        // Set initial content if valid
        if(!content.equals("")&&!content.equals("null"))mainTextField.setText(content);

        // Optionally create and add a label (below the field!)
        if(!label.equals("")) {
            Label tflabel = new Label(label);
            tflabel.setStyle("-fx-font-size: 12px;");
            parent.getChildren().add(tflabel);
        }
    }

    /**
     * Marks a TextField visually if its content is empty (e.g., by setting background color).
     *
     * @param tf the TextField to check and style
     */
    private void markTextfield(TextField tf, String s) {
        String content = tf.getText();
        if (s.equals("r")){
            if (content.equals("") || !resourceNames.contains(content)) tf.setStyle("-fx-control-inner-background: lightyellow;");
            else tf.setStyle("");  // Reset to default
        }
        else if (s.equals("t")){
            if (content.equals("") || !timetableNames.contains(content)) tf.setStyle("-fx-control-inner-background: lightyellow;");
            else tf.setStyle("");  // Reset to default
        }
        else {
            if (content.equals("")) tf.setStyle("-fx-control-inner-background: lightyellow;");
            else tf.setStyle("");  // Reset to default
        }
    }

    /**
     * Marks a ComboBox visually if its content is empty (e.g., by setting background color).
     *
     * @param cb the ComboBox to check and style
     */
    private void markComboBox(ComboBox cb) {
        if(cb.getValue() == null) cb.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        else cb.setStyle(""); // Reset to default
    }

    /**
     * Collects all task-related data from the workflow section of the UI.
     * This includes task name, ID, required resources, following tasks, and duration.
     * Also applies basic validation and styling to incomplete fields.
     *
     * @return a list of Task objects assembled from the UI input
     */
    private List<bimpTask> collectTaskData() {
        List<bimpTask> tasks = new ArrayList<>();

        for (Node node : colWf.getChildren()) {
            if (node instanceof VBox) {
                VBox taskGroupAll = (VBox) node;

                // First child: header (contains Task ID label), second: content block
                VBox taskGroup = (VBox) taskGroupAll.getChildren().get(1);
                HBox header = (HBox) taskGroupAll.getChildren().get(0);

                // Extract task name from first TextField
                TextField taskNameField = (TextField) taskGroup.getChildren().get(0);
                String taskName = taskNameField.getText();
                markTextfield(taskNameField,"");  // Highlight if empty

                // Extract task ID from header label text
                String taskID = ((Label) header.getChildren().get(0)).getText();
                taskID = taskID.substring(taskID.lastIndexOf(" ") + 1);  // Assumes format "ID: xxx"

                // --- Collect resources ---
                TextField taskResField = (TextField) taskGroup.getChildren().get(1);
                String resName = taskResField.getText();

                markTextfield(taskResField, "r");  // Highlight if empty or not defined

                // --- Collect following tasks ---
                List<FollowingTask> followingTasks = new ArrayList<>();
                Node followingTasksNode = taskGroup.getChildren().get(3);  // VBox of follow-up task lines

                VBox followingTasksContainer = (VBox) followingTasksNode;
                for (Node followingNode : followingTasksContainer.getChildren()) {
                    HBox followingHBox = (HBox) followingNode;
                    if (followingHBox.getChildren().get(0) instanceof TextField) {
                        if(followingHBox.getChildren().get(1) instanceof TextField) {
                            TextField followingTaskID = (TextField) followingHBox.getChildren().get(0);
                            TextField followingTaskRequirement = (TextField) followingHBox.getChildren().get(1);
                            followingTasks.add(new FollowingTask(followingTaskID.getText(), followingTaskRequirement.getText()));
                        }
                    }
                }
                // --- validation for condition = 100% ---
                double condSum = 0;
                for (FollowingTask ft : followingTasks) {
                    try {
                        if (!ft.getRequirement().equals("")) condSum += Double.parseDouble(ft.getRequirement());
                    } catch (NumberFormatException e) {
                        // Handle invalid number format if needed
                        e.printStackTrace();
                    }
                }
                if (condSum != 0.0 && condSum != 1.0){
                    for (Node followingNode : followingTasksContainer.getChildren()) {
                        HBox followingHBox = (HBox) followingNode;
                        if (followingHBox.getChildren().get(0) instanceof TextField) {
                            if(followingHBox.getChildren().get(1) instanceof TextField) {
                                followingHBox.getChildren().get(1).setStyle("-fx-border-color: red; -fx-border-width: 2;");
                            }
                        }
                    }
                }
                else {
                    for (Node followingNode : followingTasksContainer.getChildren()) {
                        HBox followingHBox = (HBox) followingNode;
                        if (followingHBox.getChildren().get(0) instanceof TextField) {
                            if(followingHBox.getChildren().get(1) instanceof TextField) {
                                followingHBox.getChildren().get(1).setStyle("");
                            }
                        }
                    }
                }


                // --- Collect duration ---
                HBox durationHBox = (HBox) taskGroup.getChildren().get(6);

                ComboBox distri = (ComboBox) durationHBox.getChildren().get(0);
                TextField durationValue = (TextField) durationHBox.getChildren().get(1);
                ComboBox unit = (ComboBox) durationHBox.getChildren().get(2);

                String distriString = (String) distri.getValue();
                String durationString = durationValue.getText();
                String unitString = (String) unit.getValue();

                // Highlight if all are empty
                markComboBox(distri);
                markTextfield(durationValue,"");
                markComboBox(unit);

                bimpDuration duration = new bimpDuration(distriString, durationString, unitString);

                // --- Collect cost ---
                HBox costHBox = (HBox) taskGroup.getChildren().get(8);

                TextField fixedCost = (TextField) costHBox.getChildren().get(0);
                TextField costThresh = (TextField) costHBox.getChildren().get(1);
                TextField durationThresh = (TextField) costHBox.getChildren().get(2);
                ComboBox cUnit = (ComboBox) costHBox.getChildren().get(3);

                String fixedCostString = fixedCost.getText();
                String costThreshString = costThresh.getText();
                String durationThreshString = durationThresh.getText();
                String cUnitString = (String) cUnit.getValue();

                bimpCost cost = new bimpCost(fixedCostString, costThreshString, durationThreshString, cUnitString);

                // Create and store the Task object
                bimpTask task = new bimpTask(taskName, valueOf(taskID), resName, followingTasks, duration, cost);
                tasks.add(task);
            }
        }
        return tasks;
    }

    /**
     * Collects the list of available resources from the GUI input form.
     * Also updates the global resourceNames list for validation purposes.
     *
     * @return a list of AvailableResource objects based on user input
     */
    private List<bimpAvailableResource> collectResourcesData(List<bimpTimetable> timetables) {
        List<bimpAvailableResource> availableResources = new ArrayList<>();
        resourceNames.clear();

        int cnt = 1;

        VBox resourceBox = (VBox) colRes.getChildren().get(2);

        for(Node nodeHBox : resourceBox.getChildren()) {
            if(nodeHBox instanceof HBox) {
                // Expecting structure: [TextField, TextField, TextField, TextField]
                TextField resNameField = (TextField) ((HBox) nodeHBox).getChildren().get(0);
                TextField resamountField = (TextField) ((HBox) nodeHBox).getChildren().get(1);
                TextField resCostField = (TextField) ((HBox) nodeHBox).getChildren().get(2);
                TextField resTimetableField = (TextField) ((HBox) nodeHBox).getChildren().get(3);

                String resName = resNameField.getText();
                String resAmount = resamountField.getText();
                String resCost = resCostField.getText();
                String resTimetable = resTimetableField.getText();
                String resTimetableID = "";

                for (bimpTimetable tt : timetables){
                    if (tt.getName().equals(resTimetable))resTimetableID = tt.getID();
                }

                String id = String.valueOf(cnt);

                // Only add valid (non-empty) resource entries
                if (!resName.trim().isEmpty() || !resAmount.trim().isEmpty() || !resCost.trim().isEmpty() || !resTimetable.trim().isEmpty()) {
                    availableResources.add(new bimpAvailableResource(resName, resAmount, resCost, resTimetable, resTimetableID, id));
                    cnt++;
                }

                markTextfield(resTimetableField,"t");

                // Track valid resource names for later validation
                if (!resName.trim().isEmpty()) {
                    resourceNames.add(resName);
                }
            }
        }

        return availableResources;
    }

    /**
     * Collects the list of available resources from the GUI input form.
     * Also updates the global resourceNames list for validation purposes.
     *
     * @return a list of AvailableResource objects based on user input
     */
    private List<bimpTimetable> collectTimetableData() {
        List<bimpTimetable> availableTimetables = new ArrayList<>();
        timetableNames.clear();

        int cnt = 1;

        VBox timetableBox = (VBox) colRes.getChildren().get(4);

        for(Node nodeHBox : timetableBox.getChildren()) {
            if(nodeHBox instanceof HBox) {
                // Expecting structure: [TextField, ComboBox, ComboBox, TextField, TextField]
                TextField ttNameField = (TextField) ((HBox) nodeHBox).getChildren().get(0);
                ComboBox ttStartDayBox = (ComboBox) ((HBox) nodeHBox).getChildren().get(1);
                ComboBox ttEndDayBox = (ComboBox) ((HBox) nodeHBox).getChildren().get(2);
                TextField ttStartTimeField = (TextField) ((HBox) nodeHBox).getChildren().get(3);
                TextField ttEndTimeField = (TextField) ((HBox) nodeHBox).getChildren().get(4);

                String ttName = ttNameField.getText();
                String ttStartDay = (String) ttStartDayBox.getValue();
                String ttEndDay = (String) ttEndDayBox.getValue();
                String ttStartTime = ttStartTimeField.getText();
                String ttEndTime = ttEndTimeField.getText();

                String id = String.valueOf(cnt);

                // Only add valid (non-empty) resource entries
                if (!ttName.trim().isEmpty() || !ttStartTime.trim().isEmpty() || !ttEndTime.trim().isEmpty()) {
                    availableTimetables.add(new bimpTimetable(ttName, ttStartDay, ttEndDay, ttStartTime, ttEndTime, id));
                    cnt++;
                }
                // Track valid timetables names for later validation
                if (!ttName.trim().isEmpty()) {
                    timetableNames.add(ttName);
                }
            }
        }

        return availableTimetables;
    }

    /**
     * Collects simulation-related configuration values from the GUI.
     * Includes general parameters and selected resource measurements.
     *
     * @return a Simulation object containing all collected data
     */
    private bimpSimulation collectSimulationData(){

        HBox arVBox = (HBox) colSim.getChildren().get(1);
        String arDis = (String) ((ComboBox)arVBox.getChildren().get(0)).getValue();
        String arRate = ((TextField)arVBox.getChildren().get(1)).getText();
        String arUnit = (String) ((ComboBox)arVBox.getChildren().get(2)).getValue();
        bimpArrivalRate ar = new bimpArrivalRate(arDis, arRate, arUnit);

        HBox acVBox = (HBox) colSim.getChildren().get(3);
        String acCount = ((TextField)acVBox.getChildren().get(0)).getText();
        String acExcludeStart = ((TextField)acVBox.getChildren().get(1)).getText();
        String acExcludeEnd = ((TextField)acVBox.getChildren().get(2)).getText();

        bimpArrivalCount ac = new bimpArrivalCount(acCount, acExcludeStart, acExcludeEnd);

        String startTime = ((TextField)colSim.getChildren().get(5)).getText();

        String currency = (String) ((ComboBox)colSim.getChildren().get(7)).getValue();

        return new bimpSimulation(ar, ac, startTime, currency);
    }

    /**
     * Saves simulation settings to a CSV file named 'simulation.csv' in the selected directory.
     * Converts empty strings to 'null' for CSV readability.
     *
     * @param data the Simulation object containing the data to save
     */
    private void saveSimulationToCSV(bimpSimulation data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveDirectory,"simulation.csv")))) {

            String arrivalDistribution = data.getArrivalRate().getDistribution();
            if (arrivalDistribution != null && arrivalDistribution.equals(""))arrivalDistribution = null;
            String arrivalRate = data.getArrivalRate().getRate();
            if (arrivalRate != null && arrivalRate.equals(""))arrivalRate = null;
            String arrivalUnit = data.getArrivalRate().getUnit();
            if (arrivalUnit != null && arrivalUnit.equals(""))arrivalUnit = null;

            String arrivalCount = data.getArrivalCount().getCount();
            if (arrivalCount != null && arrivalCount.equals(""))arrivalCount = null;
            String arrivalExcludeStart = data.getArrivalCount().getExcludeStart();
            if (arrivalExcludeStart != null && arrivalExcludeStart.equals(""))arrivalExcludeStart = null;
            String arrivalExcludeEnd = data.getArrivalCount().getExcludeEnd();
            if (arrivalExcludeEnd != null && arrivalExcludeEnd.equals(""))arrivalExcludeEnd = null;

            String startDT = data.getStartTime();
            if (startDT != null && startDT.equals(""))startDT = null;

            String currency = data.getCurrency();
            if (currency != null && currency.equals(""))currency = null;

            // Write each parameter as a line in the CSV
            writer.write("Arrival Rate:{" + arrivalDistribution + ";" + arrivalRate + ";" + arrivalUnit + "}");
            writer.newLine();
            writer.write("Number of Process Instances:{" + arrivalCount + "," + arrivalExcludeStart + "," + arrivalExcludeEnd + "}");
            writer.newLine();
            writer.write("Scenario Start Date and Time:" + startDT);
            writer.newLine();
            writer.write("Currency:" + currency);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the list of available resources to a CSV file named 'resources.csv'.
     *
     * @param resources the list of resources to be saved
     */
    private void saveResourcesToCSV(List<bimpAvailableResource> resources) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveDirectory,"resources.csv")))) {
            // Write the header row
            writer.write("Name, Amount, Cost, Timetable");
            writer.newLine();

            // Write each resource on a new line
            for (bimpAvailableResource resource : resources) {
                String name = resource.getName();
                if(name.equals(""))name = null;
                String amount = resource.getAmount();
                if(amount.equals(""))amount = null;
                String cost = resource.getCost();
                if(cost.equals(""))cost = null;
                String timetable = resource.getTimetable();
                if(timetable.equals(""))timetable = null;
                writer.write( name + "," + amount + "," + cost + "," + timetable);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the list of available timetables to a CSV file named 'timetables.csv'.
     *
     * @param timetables the list of timetables to be saved
     */
    private void saveTimetablesToCSV(List<bimpTimetable> timetables) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveDirectory,"timetables.csv")))) {
            // Write the header row
            writer.write("Name, StartDay, EndDay, StartTime, EndTime");
            writer.newLine();

            // Write each resource on a new line
            for (bimpTimetable timetable : timetables) {
                String name = timetable.getName();
                if(name != null && name.equals(""))name = null;
                String startDay = timetable.getStartDay();
                if(startDay != null && startDay.equals(""))startDay = null;
                String endDay = timetable.getEndDay();
                if(endDay != null && endDay.equals(""))endDay = null;
                String startTime = timetable.getStartTime();
                if(startTime != null && startTime.equals(""))startTime = null;
                String endTime = timetable.getEndTime();
                if(endTime != null && endTime.equals(""))endTime = null;
                writer.write( name + "," + startDay + "," + endDay + "," + startTime + "," + endTime);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves a list of tasks to a CSV file named 'tasks.csv' in the specified save directory.
     * Each line contains task ID, name, required resources, following tasks with requirements, and duration.
     *
     * @param tasks the list of tasks to be saved
     */
    private void saveTaskToCSV(List<bimpTask> tasks) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveDirectory,"tasks.csv")))) {
            // Write the CSV header
            writer.write("ID, Name, Required Resource, Following Tasks with Requirements, Duration, Cost");
            writer.newLine();

            // Write each task line
            for (bimpTask task : tasks) {
                StringBuilder sb = new StringBuilder();
                sb.append(task.getTaskID()).append(",");

                // Write task name or null
                if(!task.getTaskName().equals("")) sb.append(task.getTaskName());
                else sb.append("null");

                // Collect and format required resources
                String res = "" + task.getResource();
                if(res.equals("")) res="null";
                sb.append(","+ res+ ",");

                // Collect and format following tasks with requirements
                String follow = "{";
                for (FollowingTask followingTask : task.getFollowingTasks()) {
                    if(!follow.equals("{")) follow += ",";

                    String id = followingTask.getID();
                    if(id.equals("")) id = "null";
                    String requirement = followingTask.getRequirement();
                    if(requirement.equals("")) requirement = "null";

                    follow += "{" + id + "," + requirement + "}";
                }
                if(follow.equals("{")) follow="{null,null}";
                else follow += "}";
                sb.append(follow);

                // Append duration in format [x,y,z]
                sb.append(",{");
                sb.append(task.getDuration().getDistribution());
                sb.append(",");

                if (!task.getDuration().getDuration().equals(""))sb.append(task.getDuration().getDuration());
                else sb.append("null");
                sb.append(",");
                sb.append(task.getDuration().getUnit());
                sb.append("}");

                // Append cost in format [x,y,z,a]
                sb.append(",{");
                if (!task.getCost().getFixedCost().equals(""))sb.append(task.getCost().getFixedCost());
                else sb.append("null");
                sb.append(",");
                if (!task.getCost().getCostThresh().equals(""))sb.append(task.getCost().getCostThresh());
                else sb.append("null");
                sb.append(",");
                if (!task.getCost().getDurationThresh().equals(""))sb.append(task.getCost().getDurationThresh());
                else sb.append("null");
                sb.append(",");
                sb.append(task.getCost().getUnit());
                sb.append("}");

                writer.write(String.valueOf(sb));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the loading of CSV files for tasks, resources, or simulation parameters,
     * based on the filename. Delegates parsing to the appropriate helper method.
     *
     * @param file the file to load
     */
    private void loadEvent(File file){
        if (file != null) {
            String fileName = file.getName();
            BufferedReader reader;
            int i = 0;

            // Determine file type by name and load accordingly
            if(fileName.toLowerCase().contains("tasks")) {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    String line;
                    colWf.getChildren().removeIf(node -> node instanceof VBox); // Clear old task input
                    taskID = 0;
                    while ((line = reader.readLine()) != null) {
                        i++;
                        if (i != 1) loadTasks(line, i); // Skip header
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else if(fileName.toLowerCase().contains("resources")) {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        i++;
                        if (i != 1) loadResources(line); // Skip header
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else if(fileName.toLowerCase().contains("simulation")) {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    String line;
                    int counter=0;
                    while ((line = reader.readLine()) != null) {
                        loadSimulation(counter, line);
                        counter++;
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else if(fileName.toLowerCase().contains("timetables")) {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        i++;
                        if (i != 1) loadTimetables(line); // Skip header
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * Parses and loads a task entry from a single CSV line and creates its visual representation.
     * Adds the created task group to the workflow section in the GUI.
     *
     * @param line the CSV line representing a task
     * @param i the line number (used to skip headers)
     */
    private void loadTasks(String line, Integer i){
        // Regular expression that matches commas outside of curly braces
        String regex = "(?<=^|,)([^,{}]+|\\{[^{}]*\\}|\\{(?:[^{}]*\\{[^{}]*\\}[^{}]*)*\\})(?=,|$)";

        // Matcher to apply the regex
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        List<String> result = new ArrayList<>();

        // Find all matches
        while (matcher.find()) {
            result.add(matcher.group(1).trim());
        }

        Integer id = Integer.valueOf(result.get(0));
        String name = result.get(1);
        String resource = result.get(2);

        // Extract resources, following tasks, and duration arrays
        List<String> followingTasks = new ArrayList<>();
        line = result.get(3).substring(1,result.get(3).length()-1);

        Matcher matcherFoll = pattern.matcher(line);
        while (matcherFoll.find()) {
            followingTasks.add(matcherFoll.group(1).trim());
        }

        List<String> duration = new ArrayList<>();
        line = result.get(4).substring(1,result.get(4).length()-1);

        Matcher matcherDur = pattern.matcher(line);
        while (matcherDur.find()) {
            duration.add(matcherDur.group(1).trim());
        }

        List<String> cost = new ArrayList<>();
        line = result.get(5).substring(1,result.get(5).length()-1);

        Matcher matcherCost = pattern.matcher(line);
        while (matcherCost.find()) {
            cost.add(matcherCost.group(1).trim());
        }

        // Keep taskID unique
        if(taskID<=id) taskID = id+1;

        // Create and add new task UI component
        VBox newTaskGroup = createTaskGroup(colWf, noCommaAndBraces, id, name, resource, followingTasks, duration, cost);
        colWf.getChildren().add(colWf.getChildren().size() - 1, newTaskGroup); // Add before the button
    }

    /**
     * Parses and loads a resource entry from a single CSV line and populates
     * the corresponding resource input fields in the GUI.
     *
     * @param line the CSV line representing a resource
     */
    private void loadResources(String line){
        String[] content = line.split(",");
        String name = content[0];
        String amount = content[1];
        String cost = content[2];
        String timetable = content[3];

        // If the first resource input is still empty, fill it instead of adding a new one
        if(colRes.getChildren().get(2) instanceof VBox
                && ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(0)).getText().equals("")
                && ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(1)).getText().equals("")
                && ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(2)).getText().equals("")
                && ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(3)).getText().equals("")) {
            ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(0)).setText(name);
            ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(1)).setText(amount);
            ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(2)).setText(cost);
            ((TextField) ((HBox) ((VBox) colRes.getChildren().get(2)).getChildren().get(0)).getChildren().get(3)).setText(timetable);
        }
        else {
            // Otherwise, add a new resource input line
            ((VBox) colRes.getChildren().get(2)).getChildren().add(((VBox) colRes.getChildren().get(2)).getChildren().size() - 1, createResourceField((VBox)colRes.getChildren().get(2), name, amount, cost,timetable));
        }
    }

    /**
     * Parses and loads a resource entry from a single CSV line and populates
     * the corresponding resource input fields in the GUI.
     *
     * @param line the CSV line representing a timetable
     */
    private void loadTimetables(String line){
        String[] content = line.split(",");
        String name = content[0];
        String startDay = content[1];
        String endDay = content[2];
        String startTime = content[3];
        String endTime = content[4];

        // If the first resource input is still empty, fill it instead of adding a new one
        if(colRes.getChildren().get(4) instanceof VBox
                && ((TextField) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(0)).getText().equals("")
                && ((TextField) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(3)).getText().equals("")
                && ((TextField) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(4)).getText().equals("")) {
            ((TextField) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(0)).setText(name);
            ((ComboBox) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(1)).setValue(startDay);
            ((ComboBox) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(2)).setValue(endDay);
            ((TextField) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(3)).setText(startTime);
            ((TextField) ((HBox) ((VBox) colRes.getChildren().get(4)).getChildren().get(0)).getChildren().get(4)).setText(endTime);
        }
        else {
            // Otherwise, add a new resource input line
            ((VBox) colRes.getChildren().get(4)).getChildren().add(((VBox) colRes.getChildren().get(4)).getChildren().size() - 1, createTimetableField((VBox)colRes.getChildren().get(4), name, startDay, endDay, startTime, endTime));
        }
    }

    /**
     * Loads simulation parameters from a CSV line based on the expected order of settings.
     * Updates the respective simulation input fields in the GUI.
     *
     * @param counter the index of the setting (used to map the value to a specific field)
     * @param line the CSV line containing the simulation setting
     */
    private void loadSimulation(Integer counter, String line){
        String[] content1 = line.split(":");
        String content = content1[1];

        switch (counter) {
            case 0:
                String[] content2 = content.substring(1,content.length()-1).split(";");
                ((ComboBox)((HBox)colSim.getChildren().get(1)).getChildren().get(0)).setValue(content2[0]);
                ((TextField)((HBox)colSim.getChildren().get(1)).getChildren().get(1)).setText(content2[1]);
                ((ComboBox)((HBox)colSim.getChildren().get(1)).getChildren().get(2)).setValue(content2[2]);
                break;
            case 1:
                String[] content3 = content.substring(1,content.length()-1).split(",");
                ((TextField)((HBox)colSim.getChildren().get(3)).getChildren().get(0)).setText(content3[0]);
                ((TextField)((HBox)colSim.getChildren().get(3)).getChildren().get(1)).setText(content3[1]);
                ((TextField)((HBox)colSim.getChildren().get(3)).getChildren().get(2)).setText(content3[2]);
                break;
            case 2:
                if (!content.equals("null")){
                    ((TextField) colSim.getChildren().get(5)).setText(content);
                    if (content1.length>1) ((TextField) colSim.getChildren().get(5)).setText(content+":"+content1[2]);
                }
                break;
            case 3:
                ((ComboBox) colSim.getChildren().get(7)).setValue(content);
                break;
        }
    }

    private List<String[]> saveConnectionsToList(List<bimpTask> tasks) {
        // Current form: {From, To, Condition}
        List<String[]> bpmnConnectionList = new ArrayList<>();
        int gatewayCnt = 1;
        for (bimpTask t : tasks){
            if(t.getFollowingTasks().size()>1){
                String[] y = new String[]{"task" + t.getTaskID(), "gateway"+gatewayCnt, ""};
                bpmnConnectionList.add(y);

                for (FollowingTask f : t.getFollowingTasks()){
                    String[] x = new String[]{"gateway"+gatewayCnt, "task" + f.getID(), f.getRequirement()};
                    bpmnConnectionList.add(x);
                }
                gatewayCnt++;
            }
            else if(t.getFollowingTasks().size()==1){
                String[] z;
                FollowingTask f = t.getFollowingTasks().get(0);
                if(!f.getID().equals("")){
                    z = new String[]{"task" + t.getTaskID(), "task" + f.getID(), f.getRequirement()};
                }
                else {
                    z = new String[]{"task" + t.getTaskID(), "end", ""};
                }
                bpmnConnectionList.add(z);
            }
        }
        return bpmnConnectionList;
    }

    private void createBPMN(List<bimpTask> tasks, List<String[]> connections, bimpSimulation sim, List<bimpTimetable> tt, List<bimpAvailableResource> res){
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();

        // Create definitions and process
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace("http://example.com/bpmn");
        modelInstance.setDefinitions(definitions);

        Process process = modelInstance.newInstance(Process.class);
        process.setId("process");
        process.setExecutable(true);
        definitions.addChildElement(process);

        // Add start and end events
        StartEvent startEvent = modelInstance.newInstance(StartEvent.class);
        startEvent.setId("start");
        startEvent.setName("Start");
        process.addChildElement(startEvent);

        EndEvent endEvent = modelInstance.newInstance(EndEvent.class);
        endEvent.setId("end");
        endEvent.setName("End");
        process.addChildElement(endEvent);

        // Map of all flow nodes for reference
        Map<String, FlowNode> nodeMap = new HashMap<>();
        nodeMap.put("start", startEvent);
        nodeMap.put("end", endEvent);

        // Add user tasks
        for (bimpTask task : tasks) {
            String id = "task" + task.getTaskID();
            String name = task.getTaskName();
            String assignee = task.getResource();

            UserTask userTask = modelInstance.newInstance(UserTask.class);
            userTask.setId(id);
            userTask.setName(name);
            userTask.setCamundaAssignee(assignee);
            process.addChildElement(userTask);
            nodeMap.put(id, userTask);
        }

        // Add gateways if found in connections
        for (String[] conn : connections) {
            String from = conn[0];
            if (from.startsWith("gateway") && !nodeMap.containsKey(from)) {
                if (!conn[2].equals("")) {
                    ExclusiveGateway gateway = modelInstance.newInstance(ExclusiveGateway.class);
                    gateway.setId(from);
                    gateway.setName(from);
                    process.addChildElement(gateway);
                    nodeMap.put(from, gateway);
                }
                else {
                    ParallelGateway gateway = modelInstance.newInstance(ParallelGateway.class);
                    gateway.setId(from);
                    gateway.setName(from);
                    process.addChildElement(gateway);
                    nodeMap.put(from, gateway);
                }
            }


        }

        // Connect start event to first task
        modelInstance.newInstance(SequenceFlow.class);
        createSequenceFlow(modelInstance, process, startEvent, nodeMap.get("task1"), "");

        // Add all connections
        for (Object[] conn : connections) {
            String from = (String) conn[0];
            String to = (String) conn[1];
            String condition = (String) conn[2];

            FlowNode source = nodeMap.get(from);
            FlowNode target = nodeMap.get(to);

            createSequenceFlow(modelInstance, process, source, target, condition);
        }

        // add diagram info for visualization
        addHorizontalFlowLayout(modelInstance, process);

        // Save the BPMN model to file
        if (saveDirectory == null) {
            // Prompt user to select a directory
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose Save Location");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            saveDirectory = directoryChooser.showDialog(colSim.getScene().getWindow());
        }
        File file = new File(saveDirectory,"generated-BPMN.bpmn");
        Bpmn.writeModelToFile(file, modelInstance);
        System.out.println("BPMN file created: " + file.getAbsolutePath());

        try {
            addSimulationInfo(file, connections, sim, tt, res, tasks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createSequenceFlow(BpmnModelInstance modelInstance, Process process,
                                           FlowNode from, FlowNode to, String condition) {
        SequenceFlow flow = modelInstance.newInstance(SequenceFlow.class);
        String flowId = from.getId() + "_to_" + to.getId();
        flow.setId(flowId);

        // Set source and target (must already be in model)
        flow.setSource(from);
        flow.setTarget(to);

        // Add to process FIRST, before connecting
        process.addChildElement(flow);

        // Optionally add condition
        if (from instanceof Gateway && !condition.equals("")) {
            ConditionExpression conditionExpression = modelInstance.newInstance(ConditionExpression.class);
            conditionExpression.setTextContent(condition);
            flow.setConditionExpression(conditionExpression);
        }
        if (from instanceof Gateway && condition.equals("")) {
            ConditionExpression conditionExpression = modelInstance.newInstance(ConditionExpression.class);
            conditionExpression.setTextContent(condition);
            flow.setConditionExpression(conditionExpression);
        }
    }

    static void addHorizontalFlowLayout(BpmnModelInstance modelInstance, Process process) {
        BpmnDiagram diagram = modelInstance.newInstance(BpmnDiagram.class);
        diagram.setName("Process Diagram");
        BpmnPlane plane = modelInstance.newInstance(BpmnPlane.class);
        plane.setBpmnElement(process);
        diagram.addChildElement(plane);
        modelInstance.getDefinitions().addChildElement(diagram);

        int startX = 100; // Starting X position
        int startY = 300; // Y position for the main flow
        int nodeSpacingX = 200; // Horizontal spacing
        int nodeSpacingY = 150; // Vertical spacing for branches
        int nodeWidth = 100;
        int nodeHeight = 80;

        // Map to store node positions
        Map<String, Bounds> nodeBounds = new HashMap<>();

        // Find start event
        Optional<FlowNode> startEventOpt = process.getFlowElements().stream()
                .filter(fe -> fe instanceof StartEvent)
                .map(fe -> (FlowNode) fe)
                .findFirst();

        if (!startEventOpt.isPresent()) {
            // No start event found
            return;
        }

        // Queue for BFS traversal
        Queue<FlowNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Initialize with start event
        FlowNode startNode = startEventOpt.get();
        queue.add(startNode);
        visited.add(startNode.getId());

        int currentX = startX;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            int branchY = startY; // For stacking branches vertically

            for (int i = 0; i < levelSize; i++) {
                FlowNode node = queue.poll();

                // Create shape and bounds
                BpmnShape shape = modelInstance.newInstance(BpmnShape.class);
                shape.setBpmnElement(node);

                Bounds bounds = modelInstance.newInstance(Bounds.class);
                bounds.setX(currentX);
                bounds.setY(branchY);
                bounds.setWidth(nodeWidth);
                bounds.setHeight(nodeHeight);
                shape.setBounds(bounds);
                plane.addChildElement(shape);
                nodeBounds.put(node.getId(), bounds);

                // Find outgoing sequence flows
                List<SequenceFlow> outgoingFlows = process.getFlowElements().stream()
                        .filter(fe -> fe instanceof SequenceFlow)
                        .map(fe -> (SequenceFlow) fe)
                        .filter(sf -> sf.getSource().getId().equals(node.getId()))
                        .collect(Collectors.toList());

                // Enqueue targets
                for (SequenceFlow flow : outgoingFlows) {
                    FlowNode target = (FlowNode) flow.getTarget();
                    if (!visited.contains(target.getId())) {
                        queue.add(target);
                        visited.add(target.getId());
                    }
                }

                // For branching, position subsequent branches vertically
                branchY += nodeSpacingY;
            }
            // Move to next column for next level
            currentX += nodeSpacingX;
        }

        // Create edges (sequence flows)
        for (SequenceFlow flow : process.getFlowElements().stream().filter(fe -> fe instanceof SequenceFlow).map(fe -> (SequenceFlow) fe).collect(Collectors.toList())) {
            BpmnEdge edge = modelInstance.newInstance(BpmnEdge.class);
            edge.setBpmnElement(flow);

            Bounds sourceBounds = nodeBounds.get(flow.getSource().getId());
            Bounds targetBounds = nodeBounds.get(flow.getTarget().getId());


            if (sourceBounds != null && targetBounds != null) {
                Waypoint start = modelInstance.newInstance(Waypoint.class);
                start.setX(sourceBounds.getX() + sourceBounds.getWidth());
                start.setY(sourceBounds.getY() + sourceBounds.getHeight() / 2);

                Waypoint end = modelInstance.newInstance(Waypoint.class);
                end.setX(targetBounds.getX());
                end.setY(targetBounds.getY() + targetBounds.getHeight() / 2);

                edge.getWaypoints().add(start);
                edge.getWaypoints().add(end);


                if (flow.getConditionExpression() != null) {
                    String conditionExpr = flow.getConditionExpression().getTextContent();
                    // set the name of the SequenceFlow to its condition
                    flow.setName(conditionExpr);
                }

                plane.addChildElement(edge);
            }
        }
    }

    private void addSimulationInfo(File bpmnFile, List<String[]> connections, bimpSimulation sim, List<bimpTimetable> timetablesList, List<bimpAvailableResource> resourcesList, List<bimpTask> tasksList) throws Exception {
        // Load BPMN XML file with namespace awareness
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(bpmnFile);

        Element root = doc.getDocumentElement(); // <definitions>

        // Add xmlns:qbp namespace if not present
        String qbpNamespace = "http://www.qbp-simulator.com/Schema201212";



        // Create <qbp:processSimulationInfo> element with attributes
        Element simInfo = doc.createElementNS(qbpNamespace, "qbp:processSimulationInfo");
        simInfo.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:qbp", qbpNamespace);

        simInfo.setAttribute("id", "qbp_e6a1da1e-1b26-b7ae-66cd-ca891d9ea7ef");
        simInfo.setAttribute("processInstances", sim.getArrivalCount().getCount());
        String dateAndTime = timeConvert(sim.getStartTime());
        simInfo.setAttribute("startDateTime", dateAndTime);
        simInfo.setAttribute("currency", sim.getCurrency());

        String exStart = sim.getArrivalCount().getExcludeStart();
        if (exStart.equals("null"))exStart="0";
        String exEnd = sim.getArrivalCount().getExcludeEnd();
        if (exEnd.equals("null"))exEnd="0";

        Element arrivalRate = doc.createElementNS(qbpNamespace, "qbp:arrivalRateDistribution");
        arrivalRate.setAttribute("type", sim.getArrivalRate().getDistribution().toUpperCase());
        String rate = sim.getArrivalRate().getRate();
        String[] arrivalSplit = rate.split(",");
        if (arrivalSplit.length == 2){
            String dur1 = arrivalSplit[0];
            String dur2 = arrivalSplit[1];
            switch (sim.getArrivalRate().getUnit().toLowerCase()) {
                case "seconds":

                    break;
                case "minutes":
                    dur1 = String.valueOf(Integer.parseInt(dur1) * 60);
                    dur2 = String.valueOf(Integer.parseInt(dur1) * 60);
                    break;
                case "hours":
                    dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                    dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                    break;
                case "days":
                    dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                    dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                    break;
            }
            arrivalRate.setAttribute("mean", dur1);
            arrivalRate.setAttribute("arg1", dur2);
            arrivalRate.setAttribute("arg2", "NaN");
        }
        else if (arrivalSplit.length == 3){
            String dur1 = arrivalSplit[0];
            String dur2 = arrivalSplit[1];
            String dur3 = arrivalSplit[2];
            switch (sim.getArrivalRate().getUnit().toLowerCase()) {
                case "seconds":

                    break;
                case "minutes":
                    dur1 = String.valueOf(Integer.parseInt(dur1) * 60);
                    dur2 = String.valueOf(Integer.parseInt(dur1) * 60);
                    dur3 = String.valueOf(Integer.parseInt(dur1) * 60);
                    break;
                case "hours":
                    dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                    dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                    dur3 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                    break;
                case "days":
                    dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                    dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                    dur3 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                    break;
            }
            arrivalRate.setAttribute("mean", dur1);
            arrivalRate.setAttribute("arg1", dur2);
            arrivalRate.setAttribute("arg2", dur3);
        }
        else{
            switch (sim.getArrivalRate().getUnit().toLowerCase()) {
                case "seconds":
                    break;
                case "minutes":
                    rate = String.valueOf(Integer.parseInt(rate) * 60);
                    break;
                case "hours":
                    rate = String.valueOf(Integer.parseInt(rate) * 60 * 60);
                    break;
                case "days":
                    rate = String.valueOf(Integer.parseInt(rate) * 60 * 60 * 24);
                    break;
            }
            arrivalRate.setAttribute("mean", rate);
            arrivalRate.setAttribute("arg1", "NaN");
            arrivalRate.setAttribute("arg2", "NaN");
        }
        Element timeUnit = doc.createElementNS(qbpNamespace, "qbp:timeUnit");
        timeUnit.setTextContent(sim.getArrivalRate().getUnit().toLowerCase());
        arrivalRate.appendChild(timeUnit);
        simInfo.appendChild(arrivalRate);

        Element timetables = doc.createElementNS(qbpNamespace, "qbp:timetables");
        //default timetable
        Element timetableDef = doc.createElementNS(qbpNamespace, "qbp:timetable");
        timetableDef.setAttribute("id", "QBP_DEFAULT_TIMETABLE");
        timetableDef.setAttribute("default", "true");
        timetableDef.setAttribute("name", "Default");
        Element rulesDef = doc.createElementNS(qbpNamespace, "qbp:rules");
        Element ruleDef = doc.createElementNS(qbpNamespace, "qbp:rule");
        ruleDef.setAttribute("fromTime", "09:00:00.000+00:00");
        ruleDef.setAttribute("toTime", "17:00:00.000+00:00");
        ruleDef.setAttribute("fromWeekDay", "MONDAY");
        ruleDef.setAttribute("toWeekDay", "FRIDAY");
        rulesDef.appendChild(ruleDef);
        timetableDef.appendChild(rulesDef);
        timetables.appendChild(timetableDef);

        for (bimpTimetable tt : timetablesList) {
            Element timetable = doc.createElementNS(qbpNamespace, "qbp:timetable");
            timetable.setAttribute("id", "Timetable_" + tt.getID());
            timetable.setAttribute("default", "false");
            timetable.setAttribute("name", tt.getName());
            Element rules = doc.createElementNS(qbpNamespace, "qbp:rules");
            Element rule = doc.createElementNS(qbpNamespace, "qbp:rule");
            rule.setAttribute("fromTime", tt.getStartTime() + ":00.000+00:00");
            rule.setAttribute("toTime", tt.getEndTime() + ":00.000+00:00");
            rule.setAttribute("fromWeekDay", tt.getStartDay().toUpperCase());
            rule.setAttribute("toWeekDay", tt.getEndDay().toUpperCase());
            rules.appendChild(rule);
            timetable.appendChild(rules);
            timetables.appendChild(timetable);
        }
        simInfo.appendChild(timetables);

        Element resources = doc.createElementNS(qbpNamespace, "qbp:resources");
        for (bimpAvailableResource resourceInstance : resourcesList) {
            Element resource = doc.createElementNS(qbpNamespace, "qbp:resource");
            String id = "Resource" + resourceInstance.getID();
            String name = resourceInstance.getName();
            String totalAmount = resourceInstance.getAmount();
            String costPerHour = resourceInstance.getCost();
            String timetableId = "Timetable_" + resourceInstance.getTtID();
            resource.setAttribute("id",id);
            resource.setAttribute("name",name);
            resource.setAttribute("totalAmount",totalAmount);
            resource.setAttribute("costPerHour",costPerHour);
            resource.setAttribute("timetableId",timetableId);
            resources.appendChild(resource);
        }
        simInfo.appendChild(resources);

        Element elements = doc.createElementNS(qbpNamespace, "qbp:elements");
        for (bimpTask taskInstance : tasksList) {
            Element element = doc.createElementNS(qbpNamespace, "qbp:element");
            String id = "elementTask" + taskInstance.getTaskID();
            String elementId = "task" + taskInstance.getTaskID();
            String fixedCost = taskInstance.getCost().getFixedCost();
            String costThresh = taskInstance.getCost().getCostThresh();
            String durationThresh = taskInstance.getCost().getDurationThresh();

            if (fixedCost != null && !fixedCost.equals("") && !fixedCost.equals("null")){
                element.setAttribute("fixedCost", fixedCost);
            }
            if (costThresh != null && !costThresh.equals("") && !costThresh.equals("null")){
                element.setAttribute("costThreshold", costThresh);
            }
            if (durationThresh != null && !durationThresh.equals("") && !durationThresh.equals("null")){
                element.setAttribute("durationThreshold", durationThresh);
            }


            String timeUnitValue = taskInstance.getDuration().getUnit();
            if (timeUnitValue == null || timeUnitValue.equals("")||timeUnitValue.equals("null")) timeUnitValue = "seconds";
            element.setAttribute("id", id);
            element.setAttribute("elementId", elementId);

            Element durationDistribution = doc.createElementNS(qbpNamespace, "qbp:durationDistribution");
            String duration = taskInstance.getDuration().getDuration();
            durationDistribution.setAttribute("type", taskInstance.getDuration().getDistribution().toUpperCase());

            String[] durationSplit = duration.split(",");
            if (durationSplit.length == 2){
                String dur1 = durationSplit[0];
                String dur2 = durationSplit[1];
                switch (timeUnitValue.toLowerCase()) {
                    case "seconds":

                        break;
                    case "minutes":
                        dur1 = String.valueOf(Integer.parseInt(dur1) * 60);
                        dur2 = String.valueOf(Integer.parseInt(dur1) * 60);
                        break;
                    case "hours":
                        dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                        dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                        break;
                    case "days":
                        dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                        dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                        break;
                }
                durationDistribution.setAttribute("mean", dur1);
                durationDistribution.setAttribute("arg1", dur2);
                durationDistribution.setAttribute("arg2", "NaN");
            }
            else if (durationSplit.length == 3){
                String dur1 = durationSplit[0];
                String dur2 = durationSplit[1];
                String dur3 = durationSplit[2];
                switch (timeUnitValue.toLowerCase()) {
                    case "seconds":

                        break;
                    case "minutes":
                        dur1 = String.valueOf(Integer.parseInt(dur1) * 60);
                        dur2 = String.valueOf(Integer.parseInt(dur1) * 60);
                        dur3 = String.valueOf(Integer.parseInt(dur1) * 60);
                        break;
                    case "hours":
                        dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                        dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                        dur3 = String.valueOf(Integer.parseInt(dur1) * 60 * 60);
                        break;
                    case "days":
                        dur1 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                        dur2 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                        dur3 = String.valueOf(Integer.parseInt(dur1) * 60 * 60 * 24);
                        break;
                }
                durationDistribution.setAttribute("mean", dur1);
                durationDistribution.setAttribute("arg1", dur2);
                durationDistribution.setAttribute("arg2", dur3);
            }
            else{
                switch (timeUnitValue.toLowerCase()) {
                    case "seconds":
                        break;
                    case "minutes":
                        duration = String.valueOf(Integer.parseInt(duration) * 60);
                        break;
                    case "hours":
                        duration = String.valueOf(Integer.parseInt(duration) * 60 * 60);
                        break;
                    case "days":
                        duration = String.valueOf(Integer.parseInt(duration) * 60 * 60 * 24);
                        break;
                }
                durationDistribution.setAttribute("mean", duration);
                durationDistribution.setAttribute("arg1", "NaN");
                durationDistribution.setAttribute("arg2", "NaN");
            }
            Element timeUnitTask = doc.createElementNS(qbpNamespace, "qbp:timeUnit");
            timeUnitTask.setTextContent(timeUnitValue.toLowerCase());

            Element resourceIds = doc.createElementNS(qbpNamespace, "qbp:resourceIds");
            Element resourceId = doc.createElementNS(qbpNamespace, "qbp:resourceId");
            String resourceID = "";

            for (bimpAvailableResource resourceInstance : resourcesList) {
                if(resourceInstance.getName().equals(taskInstance.getResource())) resourceID = "Resource" + resourceInstance.getID();
            }


            resourceId.setTextContent(resourceID);

            durationDistribution.appendChild(timeUnitTask);
            element.appendChild(durationDistribution);
            resourceIds.appendChild(resourceId);
            element.appendChild(resourceIds);
            String durationThresholdTimeUnit = taskInstance.getCost().getUnit();

            if (durationThresholdTimeUnit != null && !durationThresholdTimeUnit.equals("") && !durationThresholdTimeUnit.equals("null")){
                Element durationThresholdTimeUnitElement = doc.createElementNS(qbpNamespace, "qbp:durationThresholdTimeUnit");
                durationThresholdTimeUnitElement.setTextContent(durationThresholdTimeUnit.toLowerCase());
                element.appendChild(durationThresholdTimeUnitElement);
            }

            elements.appendChild(element);
        }
        simInfo.appendChild(elements);
        Element sequenceFlows = doc.createElementNS(qbpNamespace, "qbp:sequenceFlows");
        int cnt = 0;
        for (String[] connectionInstance : connections) {
            Element sequenceFlow = doc.createElementNS(qbpNamespace, "qbp:sequenceFlow");
            if (connectionInstance[0].startsWith("gateway") && !connectionInstance[2].equals("")) {
                String elementId = connectionInstance[0] + "_to_" + connectionInstance[1];
                sequenceFlow.setAttribute("elementId", elementId);
                String executionProbability = connectionInstance[2];
                sequenceFlow.setAttribute("executionProbability", executionProbability);
                sequenceFlows.appendChild(sequenceFlow);
                cnt++;
            }
        }
        if (cnt == 0){  // element needed at least once even if empty
            Element sequenceFlow = doc.createElementNS(qbpNamespace, "qbp:sequenceFlow");
            sequenceFlows.appendChild(sequenceFlow);
        }

        simInfo.appendChild(sequenceFlows);


        Element statsOptions = doc.createElementNS(qbpNamespace, "qbp:statsOptions");
        if (sim.getArrivalCount().getExcludeStart() != null && !sim.getArrivalCount().getExcludeStart().equals("null") && !sim.getArrivalCount().getExcludeStart().equals("")){
            statsOptions.setAttribute("trimStartProcessInstances",sim.getArrivalCount().getExcludeStart());
        }

        if (sim.getArrivalCount().getExcludeEnd() != null && !sim.getArrivalCount().getExcludeEnd().equals("null") && !sim.getArrivalCount().getExcludeEnd().equals("")){
            statsOptions.setAttribute("trimStartProcessInstances",sim.getArrivalCount().getExcludeEnd());
        }

        simInfo.appendChild(statsOptions);

        // Find the <bpmndi:BPMNDiagram> element
        NodeList diagrams = doc.getElementsByTagNameNS("*", "BPMNDiagram");
        if (diagrams.getLength() == 0) {
            throw new RuntimeException("No BPMNDiagram element found in BPMN file!");
        }
        org.w3c.dom.Node diagram = diagrams.item(0);
        org.w3c.dom.Node parent = diagram.getParentNode();

        // Insert simInfo AFTER the <bpmndi:BPMNDiagram> element
        org.w3c.dom.Node next = diagram.getNextSibling();
        if (next != null) {
            parent.insertBefore(simInfo, next);
        } else {
            parent.appendChild(simInfo);
        }

        // Save the updated document back to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(bpmnFile);
        transformer.transform(source, result);

        System.out.println("Simulation info inserted successfully.");
    }
    private static String timeConvert(String dt) {
        try {
        // Parse as LocalDateTime
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(dt, inputFormatter);

        // Assign German time zone (e.g., Europe/Berlin)
        ZonedDateTime berlinTime = localDateTime.atZone(ZoneId.of("Europe/Berlin"));

        // Convert to UTC
        ZonedDateTime utcTime = berlinTime.withZoneSameInstant(ZoneId.of("UTC"));

        // Format to desired ISO 8601 with 'Z'
        return utcTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        } catch (DateTimeParseException e) {
            //Default: A Monday at 6am
            return"2025-06-09T04:00:00.000Z";
        }
    }
}
