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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Integer.valueOf;


// Main JavaFX application class for the dynamic input form
public class InputFormBIMP extends Application {
    // VBox containers for organizing the UI into three vertical sections:
    private VBox colWf;
    private VBox colRes;
    private VBox colSim;
    // Counter to assign unique IDs to newly created tasks
    private Integer taskID = 1;
    // List to keep track of all defined resource names (used for assignment in tasks)
    private List<String> resourceNames = new ArrayList<>();
    // Directory where the current input form data will be saved
    private File saveDirectory = null;
    // ChangeListener to enforce numeric-only input in TextFields
    // Reverts the input to the previous value if the new value contains non-digit characters
    private final ChangeListener<String> numeric = (observable, oldValue, newValue) -> {
        if (newValue.matches("\\d*")) return;
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
        VBox firstTextFieldGroup = createTaskGroup(colWf, noCommaAndBraces, taskID,"", List.of(), List.of(), List.of());
        colWf.getChildren().add(firstTextFieldGroup);
        taskID++;

        // Button to dynamically add new task groups
        Button addGroupButton = new Button("Add New Task");
        addGroupButton.setOnAction(event -> {
            // Create a new task group with empty initial values
            VBox newTaskGroup = createTaskGroup(colWf, noCommaAndBraces, taskID,"", List.of(), List.of(), List.of());
            // Insert it before the button itself
            colWf.getChildren().add(colWf.getChildren().size() - 1, newTaskGroup);
            taskID++;
        });

        // Add the task-adding button to the end of the workflow column
        colWf.getChildren().add(addGroupButton);


        //----------------------Resource----------------------
        // Initialize the resource column VBox with title "Resources"
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
        HBox firstTimetableContainer = createTimetableField(descriptionsHBox2, "", "", "", "");

        // Button to dynamically add more resource input rows
        Button addTimetableButton = new Button("+");
        addTimetableButton.setOnAction(event -> {
            HBox newTextFieldContainer = createTimetableField(descriptionsHBox2, "", "", "", "");
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
        Tooltip tooltipDis = new Tooltip("Distribution of arrival time");
        tooltipDis.setStyle("-fx-font-size: 14px;");
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
        final ComboBox comboBoxDis = new ComboBox(optionsDis);
        comboBoxDis.setPrefWidth(130);
        comboBoxDis.setTooltip(tooltipDis);
        comboBoxDis.setPromptText("Inter arrival time");
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

        Tooltip tooltipUnit = new Tooltip("Time unit for the values");
        tooltipUnit.setStyle("-fx-font-size: 14px;");
        ObservableList<String> optionsUnit =
                FXCollections.observableArrayList(
                        "Seconds",
                        "Minutes",
                        "Hours",
                        "Days"
                );
        final ComboBox comboBoxUnit = new ComboBox(optionsUnit);
        comboBoxUnit.setPrefWidth(80);
        comboBoxUnit.setTooltip(tooltipUnit);
        comboBoxUnit.setPromptText("Unit");
        Label cblabelUnit = new Label("Distribution");
        cblabelUnit.setStyle("-fx-font-size: 12px;");

        HBox atHBox = new HBox(10);
        atHBox.getChildren().addAll(comboBoxDis, durTextField, comboBoxUnit);

        TextField startTextField = new TextField();
        startTextField.setPromptText("Month XXth HH:MM");
        Tooltip tooltipST = new Tooltip("Specifiy the simulation start month day and time (e.g. 'May 24th 09:00').");
        tooltipST.setStyle("-fx-font-size: 14px;");
        Label startlabel = new Label("Scenario start date and time");
        startlabel.setStyle("-fx-font-size: 12px;");
        startTextField.setPrefWidth(120);

        HBox piHBox = new HBox(60);
        Label piLabel = new Label("Total number of process instances.       % to exclude form stats");
        // Total number of entities to arrive during the simulation
        String tooltipString = "Set the total number of process instances to be created during the simulation (e.g., '100' means the simulation stops after 100 arrivals).";
        createTextfieldHBox(piHBox, "", "Total Arrival Count", "", 300, numeric, tooltipString);
        tooltipString = "Specifies the percentage of process instance performance stats to exclude from the start and the end of the simulation scenario. Use to exclude statistics from process inctances when scenario is 'warming up' or 'cooling down'.";
        createTextfieldHBox(piHBox, "", "%start, %end", "", 300, numeric, tooltipString);

        // Input for execution mode (e.g., virtual vs real-time);
        Tooltip tooltipCurr = new Tooltip("Currency is information only. Used in the simulation results page.");
        tooltipCurr.setStyle("-fx-font-size: 14px;");
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
        final ComboBox comboBoxCurr = new ComboBox(options);
        comboBoxCurr.setTooltip(tooltipCurr);
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
            List<AvailableResource> resources = collectResourcesData();
            saveResourcesToCSV(resources);

            List<Task> tasks = collectTaskData();
            saveTaskToCSV(tasks);

            Simulation sim = collectSimulationData();
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
            List<Task> tasks = collectTaskData();
            createBPMN(saveTaskToList(tasks),saveConnectionsToList(tasks));
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
    private VBox createTaskGroup(VBox parent, ChangeListener x, Integer taskID, String name, List<String> res, List<String> fTasks, List<String> dur) {
        VBox textFieldGroupBox = new VBox(10);
        VBox textFieldGroup = new VBox(10);
        textFieldGroup.setAlignment(Pos.TOP_LEFT);
        textFieldGroup.setStyle("-fx-border-color: gray; -fx-border-width: 2px; -fx-padding: 5px;");

        // Task name input
        TextField firstTextField = new TextField();
        firstTextField.textProperty().addListener(x);
        firstTextField.setMaxWidth(550);
        firstTextField.setPromptText("Task Name");
        if(!name.equals("")&&!name.equals("null")) firstTextField.setText(name);
        Tooltip tooltip = new Tooltip("Enter a short, descriptive name for the task (e.g., 'Review Application' or 'Approve Invoice'). This name will help identify the task in the process flow.");
        tooltip.setStyle("-fx-font-size: 14px;");
        firstTextField.setTooltip(tooltip);

        // ------------------ Required Resources Section ------------------
        //Label requiredLabel = new Label("Required Resources");
        //requiredLabel.setStyle("-fx-font-size: 12px;");
        VBox resourceLinesContainer = new VBox(10);

        String firstResourceLineName = "";
        //String firstResourceLineAmount = "";

        // Load existing resource lines
        if(res.size()>0) {
            firstResourceLineName = res.get(0).substring(1,res.get(0).length()-1).split(",")[0];
            //firstResourceLineAmount = res.get(0).substring(1,res.get(0).length()-1).split(",")[1];
        }
        HBox firstResourceLine = createResourceLine(noCommaAndBraces,resourceLinesContainer,firstResourceLineName);
        resourceLinesContainer.getChildren().add(firstResourceLine);


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
        Tooltip tooltipDis = new Tooltip("Distribution of duration");
        tooltipDis.setStyle("-fx-font-size: 14px;");
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
        final ComboBox comboBoxDis = new ComboBox(optionsDis);
        comboBoxDis.setPrefWidth(110);
        comboBoxDis.setTooltip(tooltipDis);
        comboBoxDis.setPromptText("Distribution");
        Label cblabelDis = new Label("Distribution");
        cblabelDis.setStyle("-fx-font-size: 12px;");

        Label durationLabel = new Label("Duration");
        durationLabel.setStyle("-fx-font-size: 12px;");
        HBox timeHBox = new HBox(10);

        Tooltip tooltipUnit = new Tooltip("Time unit for the values");
        tooltipUnit.setStyle("-fx-font-size: 14px;");
        ObservableList<String> optionsUnit =
                FXCollections.observableArrayList(
                        "Seconds",
                        "Minutes",
                        "Hours",
                        "Days"
                );
        final ComboBox comboBoxUnit = new ComboBox(optionsUnit);
        comboBoxUnit.setPrefWidth(80);
        comboBoxUnit.setTooltip(tooltipUnit);
        comboBoxUnit.setPromptText("Unit");
        Label cblabelUnit = new Label("Distribution");
        cblabelUnit.setStyle("-fx-font-size: 12px;");

        TextField durTextField = new TextField();
        durTextField.setPromptText("");
        durTextField.setPrefWidth(170);

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
        createTextfieldHBox(costHBox, "", "Fixed cost", "", 90, numeric, "Fixed cost of Task. For decimal point should be used.");
        createTextfieldHBox(costHBox, "", "Cost threshold", "", 90, numeric, "If set, then additional statistics will be generated for the tasks exceeding threshold value. Includes fixed cost. For decimal place, point should be used.");
        createTextfieldHBox(costHBox, "", "Duration threshold", "", 90, numeric, "If set, then additional statistics will be generated for the tasks exceeding threshold value. For decimal place, point should be used.");
        Tooltip tooltipCost = new Tooltip("Choose how the simulation should run: 'Virtual Time' runs as fast as possible, 'Real Time' follows actual clock time.");
        tooltipCost.setStyle("-fx-font-size: 14px;");
        ObservableList<String> optionsCost =
                FXCollections.observableArrayList(
                        "Seconds",
                        "Minutes",
                        "Hours",
                        "Days"
                );
        final ComboBox comboBoxCost = new ComboBox(optionsCost);
        comboBoxCost.setTooltip(tooltipUnit);
        comboBoxCost.setPrefWidth(80);
        comboBoxCost.setPromptText("Unit");
        Label cblabelCost = new Label("Distribution");
        cblabelCost.setStyle("-fx-font-size: 12px;");
        costHBox.getChildren().add(comboBoxCost);

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
        textFieldGroup.getChildren().addAll(firstTextField, resourceLinesContainer,
                followingTasksLabel, followingTaskLinesContainer, addLineButtonTwo,
                durationLabel, timeHBox, costLabel, costHBox, deleteButtonContainer);

        // Combine header and group
        textFieldGroupBox.getChildren().addAll(header, textFieldGroup);

        return textFieldGroupBox;
    }

    /**
     * Creates a new resource input line (HBox) with fields for resource name and amount.
     * Includes a delete button to remove the line from the given VBox parent.
     *
     * @param parent the container VBox to which the new resource line is added
     * @param name the pre-filled content for the resource name field
     */
    private HBox createResourceLine(ChangeListener x, VBox parent, String name) {
        HBox line = new HBox(10);  // Horizontal box with 10px spacing between elements
        // Main TextField for resource name
        TextField mainTextField = new TextField();
        mainTextField.setPromptText("Resource Name");
        mainTextField.setPrefWidth(550);
        mainTextField.textProperty().addListener(x);
        if(!name.equals("")&&!name.equals("null")) mainTextField.setText(name);
        Tooltip tooltip = new Tooltip("Specify the name of the resource needed to perform this task (e.g., 'Employee', 'Machine A'). Make sure it is listed in the Resource column!");
        tooltip.setStyle("-fx-font-size: 14px;");
        mainTextField.setTooltip(tooltip);

        // Assemble the line
        line.getChildren().addAll(mainTextField);

        return line;
    }

    /**
     * Creates a new line for specifying a following task and its requirement.
     * Adds the line to the given VBox parent.
     *
     * @param parent the container VBox to which the next task line is added
     * @param nextID the ID of the following task
     * @param req the requirement/condition for the following task
     */
    private HBox createNextTaskLine(ChangeListener x, VBox parent, String nextID, String req) {
        HBox line = new HBox(10);  // Horizontal box with 10px spacing

        // TextField for task ID (numeric input only)
        TextField mainTextField = new TextField();
        mainTextField.setPromptText("Task ID");
        mainTextField.setPrefWidth(60);
        if(!nextID.equals("")&&!nextID.equals("null")) mainTextField.setText(nextID);
        mainTextField.textProperty().addListener(numeric);
        Tooltip tooltip = new Tooltip("List the ID(s) of the task(s) that come immediately after this one (e.g., '1', '2'). It is found above the Task Name.");
        tooltip.setStyle("-fx-font-size: 14px;");
        mainTextField.setTooltip(tooltip);

        // TextField for the requirement description
        TextField requirements = new TextField();
        requirements.textProperty().addListener(x);
        requirements.setPromptText("% Condition");
        requirements.setPrefWidth(100);
        if(!req.equals("")&&!req.equals("null")) requirements.setText(req);
        Tooltip tooltip2 = new Tooltip("In percentage only! All conditions have to sum up to 100. If one following task has a condition all other have to have one too. Empty = Parallel, Condition = Exclusive");
        tooltip2.setStyle("-fx-font-size: 14px;");
        requirements.setTooltip(tooltip2);

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
    private HBox createResourceField(VBox parent, String c1, String c2, String c3, String c4) {
        HBox textFieldContainer = new HBox(10);  // 10px spacing
        textFieldContainer.setAlignment(Pos.BOTTOM_LEFT);  // Align children to bottom left

        // TextField for the resource name
        TextField firstTextField = new TextField();
        firstTextField.setPrefWidth(130);
        firstTextField.setText(c1);
        firstTextField.setPromptText("Name");
        Tooltip tooltip = new Tooltip("Enter the name of the resource that can be used in tasks (e.g., 'Employee', 'Forklift', 'Approval Software'). Use consistent names across all tasks.");
        tooltip.setStyle("-fx-font-size: 14px;");
        firstTextField.setTooltip(tooltip);

        // TextField for the amount
        TextField secoundTextField = new TextField();
        secoundTextField.setPrefWidth(100);
        secoundTextField.textProperty().addListener(numeric);
        secoundTextField.setText(c2);
        secoundTextField.setPromptText("# of Resources");
        Tooltip tooltip2 = new Tooltip("Specify how many units of this resource are available in total for the process (e.g., '3' employees, '2' forklifts).");
        tooltip2.setStyle("-fx-font-size: 14px;");
        secoundTextField.setTooltip(tooltip2);

        // TextField for the cost
        TextField thirdTextField = new TextField();
        thirdTextField.setPrefWidth(90);
        thirdTextField.textProperty().addListener(numeric);
        thirdTextField.setText(c3);
        thirdTextField.setPromptText("Cost per Hour");
        Tooltip tooltip3 = new Tooltip("Specify the cost per hour for this resource.");
        tooltip3.setStyle("-fx-font-size: 14px;");
        //Tooltip tooltip3 = new Tooltip("");
        //tooltip3.setStyle("-fx-font-size: 14px;");
        thirdTextField.setTooltip(tooltip3);

        // TextField for the cost
        TextField fourthTextField = new TextField();
        fourthTextField.setMaxWidth(90);
        fourthTextField.textProperty().addListener(numeric);
        Tooltip tooltip4 = new Tooltip("Enter the name of the timetable for this resource.");
        tooltip4.setStyle("-fx-font-size: 14px;");
        fourthTextField.setText(c4);
        fourthTextField.setPromptText("Timetable");
        //Tooltip tooltip4 = new Tooltip("");
        //tooltip4.setStyle("-fx-font-size: 14px;");
        fourthTextField.setTooltip(tooltip4);

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
     * @param c1 the pre-filled resource name
     * @param c2 the pre-filled amount
     * @param c3 the pre-filled cost per hour
     * @param c4 the pre-filled timetalbe
     */
    private HBox createTimetableField(VBox parent, String c1, String c2, String c3, String c4) {
        HBox textFieldContainer = new HBox(7);  // 10px spacing
        textFieldContainer.setAlignment(Pos.BOTTOM_LEFT);  // Align children to bottom left

        // TextField for the timetable name
        TextField firstTextField = new TextField();
        firstTextField.setMaxWidth(130);
        firstTextField.setText(c1);
        firstTextField.setPromptText("Name");
        Tooltip tooltip = new Tooltip("Enter the name of the timetable.");
        tooltip.setStyle("-fx-font-size: 14px;");
        firstTextField.setTooltip(tooltip);

        // combobox for the begin day
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
        final ComboBox comboBoxBday = new ComboBox(optionsBday);
        comboBoxBday.setPrefWidth(90);
        comboBoxBday.setTooltip(tooltip);
        comboBoxBday.setPromptText("Begin day");
        Tooltip tooltip2 = new Tooltip("Specify the first day of the week when the resource is available.");
        tooltip2.setStyle("-fx-font-size: 14px;");
        comboBoxBday.setTooltip(tooltip2);

        // combobox for the end day
        ObservableList<String> optionsEday =
                FXCollections.observableArrayList(
                        "Monday",
                        "Tuesday",
                        "Wednesday",
                        "Thursday",
                        "Friday",
                        "Saturday",
                        "Sunday"
                );
        final ComboBox comboBoxEday = new ComboBox(optionsBday);
        comboBoxEday.setPrefWidth(90);
        comboBoxEday.setTooltip(tooltip);
        comboBoxEday.setPromptText("End day");
        Tooltip tooltip3 = new Tooltip("Specify the last day of the week when the resource is available.");
        tooltip3.setStyle("-fx-font-size: 14px;");
        comboBoxEday.setTooltip(tooltip3);

        // TextField for the begin time
        TextField fourthTextField = new TextField();
        fourthTextField.setMaxWidth(60);
        fourthTextField.textProperty().addListener(numeric);
        Tooltip tooltip4 = new Tooltip("Time of day when resource becomes available HH:MM.");
        tooltip4.setStyle("-fx-font-size: 14px;");
        fourthTextField.setText(c4);
        fourthTextField.setPromptText("HH:MM");
        //Tooltip tooltip4 = new Tooltip("");
        //tooltip4.setStyle("-fx-font-size: 14px;");
        fourthTextField.setTooltip(tooltip4);

        // TextField for the end time
        TextField fifthTextField = new TextField();
        fifthTextField.setMaxWidth(60);
        fifthTextField.textProperty().addListener(numeric);
        Tooltip tooltip5 = new Tooltip("Time of day when resource becomes unavailable HH:MM.");
        tooltip5.setStyle("-fx-font-size: 14px;");
        fifthTextField.setText(c4);
        fifthTextField.setPromptText("HH:MM");
        //Tooltip tooltip4 = new Tooltip("");
        //tooltip4.setStyle("-fx-font-size: 14px;");
        fifthTextField.setTooltip(tooltip5);


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
     * Creates a simple TextField with a prompt, content, and a change listener.
     * Adds the field to the specified HBox parent.
     *
     * @param parent the container HBox to which the TextField is added
     * @param label the initial content of the Label
     * @param prompt the placeholder text
     * @param width the maximum width of the TextField
     * @param L the ChangeListener to observe text changes
     */
    private void createTextfield(VBox parent, String prompt, String label, double width, ChangeListener L, String tooltipString){
        // TextField with given prompt and width
        TextField mainTextField = new TextField();
        mainTextField.setPromptText(prompt);
        mainTextField.setMaxWidth(width);
        if(L.equals(numeric)) mainTextField.textProperty().addListener(L);
        Tooltip tooltip = new Tooltip(tooltipString);
        tooltip.setStyle("-fx-font-size: 14px;");
        mainTextField.setTooltip(tooltip);

        // Label to appear above the TextField
        Label tflabel = new Label(label);
        tflabel.setStyle("-fx-font-size: 12px;");

        // Add label and TextField to the parent container
        parent.getChildren().addAll(tflabel, mainTextField);
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
        TextField mainTextField = new TextField();
        mainTextField.setPromptText(prompt);                // Set placeholder text
        mainTextField.setMaxWidth(width);                   // Set maximum width
        parent.getChildren().add(mainTextField);            // Add TextField to the given HBox
        mainTextField.textProperty().addListener(L);        // Attach listener (e.g., to enforce numeric input)
        Tooltip tooltip = new Tooltip(tooltipString);
        tooltip.setStyle("-fx-font-size: 14px;");
        mainTextField.setTooltip(tooltip);

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
     * Creates a horizontal line (HBox) containing a TextField for a resource to be measured
     * and a delete button to remove the line.
     *
     * @param parent the VBox to which this line will be added
     * @param content the initial content of the resource measurement TextField
     * @return the created HBox line
     */
    private HBox createResourceMeasureFieldLine(VBox parent, String content) {
        HBox line = new HBox(10);                                     // 10px spacing between children
        TextField mainTextField = new TextField();
        mainTextField.setPromptText("Resource");                      // Placeholder for resource name
        mainTextField.setMaxWidth(300);                               // Limit field width
        mainTextField.setText(content);                               // Set initial value

        // Delete button to remove this line from the parent VBox
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> parent.getChildren().remove(line));

        // Add components to line
        line.getChildren().addAll(mainTextField, deleteButton);
        return line;
    }

    /**
     * Marks a TextField visually if its content is empty (e.g., by setting background color).
     *
     * @param tf the TextField to check and style
     */
    private void markTextfield(TextField tf) {
        String content = tf.getText();
        if(content.equals("")) tf.setStyle("-fx-control-inner-background: lightyellow;");
        else tf.setStyle("");  // Reset to default
    }

    /**
     * Collects all task-related data from the workflow section of the UI.
     * This includes task name, ID, required resources, following tasks, and duration.
     * Also applies basic validation and styling to incomplete fields.
     *
     * @return a list of Task objects assembled from the UI input
     */
    private List<Task> collectTaskData() {
        List<Task> tasks = new ArrayList<>();

        for (Node node : colWf.getChildren()) {
            if (node instanceof VBox) {
                VBox taskGroupAll = (VBox) node;

                // First child: header (contains Task ID label), second: content block
                VBox taskGroup = (VBox) taskGroupAll.getChildren().get(1);
                HBox header = (HBox) taskGroupAll.getChildren().get(0);

                // Extract task name from first TextField
                TextField taskNameField = (TextField) taskGroup.getChildren().get(0);
                String taskName = taskNameField.getText();
                markTextfield(taskNameField);  // Highlight if empty

                // Extract task ID from header label text
                String taskID = ((Label) header.getChildren().get(0)).getText();
                taskID = taskID.substring(taskID.lastIndexOf(" ") + 1);  // Assumes format "ID: xxx"

                // --- Collect resources ---
                List<Resource> resources = new ArrayList<>();
                Node resourcesNode = taskGroup.getChildren().get(2);  // VBox with multiple HBox resource lines
                if (resourcesNode instanceof VBox) {
                    VBox resourceLinesContainer = (VBox) resourcesNode;
                    for (Node lineNode : resourceLinesContainer.getChildren()) {
                        if (lineNode instanceof HBox) {
                            HBox line = (HBox) lineNode;
                            TextField resourceNameField = (TextField) line.getChildren().get(0);
                            TextField amountField = (TextField) line.getChildren().get(1);
                            String rName = resourceNameField.getText();
                            String rAmount = amountField.getText();

                            resources.add(new Resource(rName, rAmount));

                            // Highlight unknown resource names
                            if(!rName.equals("")&&!resourceNames.contains(rName)){
                                resourceNameField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                                ((Label)taskGroup.getChildren().get(1)).setText("Required Resources (red = not in Resources List)");
                            } else {
                                markTextfield(resourceNameField);  // Normal highlighting if known
                                ((Label)taskGroup.getChildren().get(1)).setText("Required Resources");
                            }
                            markTextfield(amountField);  // Highlight amount if empty
                        }
                    }
                }

                // --- Collect following tasks ---
                List<FollowingTask> followingTasks = new ArrayList<>();
                Node followingTasksNode = taskGroup.getChildren().get(5);  // VBox of follow-up task lines

                if (followingTasksNode instanceof VBox) {
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
                }

                // --- Collect duration ---
                List<Integer> durationList = new ArrayList<>();
                HBox durationHBox = (HBox) taskGroup.getChildren().get(8);  // [hour, min, sec]

                TextField durationH = (TextField) durationHBox.getChildren().get(0);
                TextField durationMin = (TextField) durationHBox.getChildren().get(1);
                TextField durationSec = (TextField) durationHBox.getChildren().get(2);

                durationList.add(valueOf("0"+durationH.getText()));    // Leading "0" ensures parseable
                durationList.add(valueOf("0"+durationMin.getText()));
                durationList.add(valueOf("0"+durationSec.getText()));

                // Highlight if all are empty
                if(durationH.getText().equals("") && durationMin.getText().equals("") && durationSec.getText().equals("")) {
                    markTextfield(durationH);
                    markTextfield(durationMin);
                    markTextfield(durationSec);
                }
                else {
                    durationH.setStyle("");
                    durationMin.setStyle("");
                    durationSec.setStyle("");
                }

                // Create and store the Task object
                Task task = new Task(taskName, valueOf(taskID), resources, followingTasks, durationList);
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
    private List<AvailableResource> collectResourcesData() {
        List<AvailableResource> availableResources = new ArrayList<>();
        resourceNames.clear();

        // Start after headers: skip the first two elements in colRes
        int cnt = 1;
        for (int i = 2; i < colRes.getChildren().size(); i++) {
            Node node = colRes.getChildren().get(i);
            if (node instanceof HBox) {
                HBox resourceBox = (HBox) node;


                // Expecting structure: [TextField, TextField, CheckBox, Button]
                if (resourceBox.getChildren().size() == 4) {
                    TextField resourceNameField = (TextField) resourceBox.getChildren().get(0);
                    TextField amountField = (TextField) resourceBox.getChildren().get(1);
                    CheckBox reusableCBox = (CheckBox) resourceBox.getChildren().get(2);

                    String resourceName = resourceNameField.getText();
                    String amount = amountField.getText();
                    Boolean reusable = reusableCBox.isSelected();
                    String id = String.valueOf(cnt);

                    // Only add valid (non-empty) resource entries
                    if (!resourceName.trim().isEmpty() || !amount.trim().isEmpty()) {
                        availableResources.add(new AvailableResource(resourceName, amount, reusable, id));
                        cnt++;
                    }
                    // Track valid resource names for later validation
                    if (!resourceName.trim().isEmpty()){
                        resourceNames.add(resourceName);
                    }
                }
            }
        }
        return availableResources;
    }

    /**
     * Collects simulation-related configuration values from the GUI.
     * Includes general parameters and selected resource measurements.
     *
     * @return a Simulation object containing all collected data
     */
    private Simulation collectSimulationData(){
        TextField arrivalRate = (TextField) colSim.getChildren().get(2);
        String arString = arrivalRate.getText();

        TextField agentsPerArrival = (TextField) colSim.getChildren().get(4);
        String apaString = agentsPerArrival.getText();

        TextField arrivalCount = (TextField) colSim.getChildren().get(6);
        String acString = arrivalCount.getText();

        // Duration is stored in a nested VBox -> HBox
        VBox durationVBox = (VBox) colSim.getChildren().get(7);
        HBox durationHBox = (HBox) durationVBox.getChildren().get(1);
        TextField durationH = (TextField) durationHBox.getChildren().get(0);
        TextField durationMin = (TextField) durationHBox.getChildren().get(1);
        TextField durationSec = (TextField) durationHBox.getChildren().get(2);

        ArrayList<String> durationArray = new ArrayList<>();
        durationArray.add(durationH.getText());
        durationArray.add(durationMin.getText());
        durationArray.add(durationSec.getText());

        ComboBox mode = (ComboBox) colSim.getChildren().get(9);
        String modeString = (String) mode.getValue();

        TextField seed = (TextField) colSim.getChildren().get(11);
        String seedString = seed.getText();

        VBox measurements = (VBox) colSim.getChildren().get(13);
        ArrayList<String> resourcesToMeasure = new ArrayList<>();
        for (Node node : measurements.getChildren()) {
            if(node instanceof TextField){
                resourcesToMeasure.add(((TextField) node).getText());

            } else if (node instanceof HBox) {
                TextField toMeasureName = (TextField) ((HBox) node).getChildren().get(0);
                resourcesToMeasure.add(toMeasureName.getText());
            }
        }
        return new Simulation(arString, apaString, acString, durationArray, modeString, seedString, resourcesToMeasure);
    }

    /**
     * Saves simulation settings to a CSV file named 'simulation.csv' in the selected directory.
     * Converts empty strings to 'null' for CSV readability.
     *
     * @param data the Simulation object containing the data to save
     */
    private void saveSimulationToCSV(Simulation data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveDirectory,"simulation.csv")))) {
            String arrivalRate = data.getArrivalRate();
            if(arrivalRate.equals("")) arrivalRate = null;
            String agentsPerArrival = data.getAgentsPerArrival();
            if(agentsPerArrival.equals("")) agentsPerArrival = null;
            String arrivalCount = data.getArrivalCount();
            if(arrivalCount.equals("")) arrivalCount = null;
            ArrayList<String> duration = data.getDuration();

            String mode = data.getMode();

            String seed = data.getSeed();
            if(seed.equals("")) seed = null;

            String resToMes = "";

            // Write each parameter as a line in the CSV
            writer.write("Arrival Rate:" + arrivalRate);
            writer.newLine();
            writer.write("Agents Per Arrival:" + agentsPerArrival);
            writer.newLine();
            writer.write("Arrival Count:" + arrivalCount);
            writer.newLine();
            writer.write("Duration:" +
                            (Integer.valueOf("0" + duration.get(0))) + "," +
                            (Integer.valueOf("0" + duration.get(1))) + "," +
                            (Integer.valueOf("0" + duration.get(2))));
            writer.newLine();
            writer.write("Time Mode:" + mode);
            writer.newLine();
            writer.write("Seed:" + seed);
            writer.newLine();
            writer.write("Resources To Measure:");

            // Collect valid resources into a comma-separated list
            for (String res : data.getResToMes()) {
                if(!res.equals("")) {
                    if(!resToMes.equals("")) resToMes +=",";
                    resToMes += res;
                }
            }
            if(resToMes.equals("")) resToMes="null";
            writer.write(resToMes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the list of available resources to a CSV file named 'resources.csv'.
     *
     * @param resources the list of resources to be saved
     */
    private void saveResourcesToCSV(List<AvailableResource> resources) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveDirectory,"resources.csv")))) {
            // Write the header row
            writer.write("Name, Amount, Reusable");
            writer.newLine();

            // Write each resource on a new line
            for (AvailableResource resource : resources) {
                writer.write(resource.getName() + "," + resource.getAmount() + "," + resource.getReusable());
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
    private void saveTaskToCSV(List<Task> tasks) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveDirectory,"tasks.csv")))) {
            // Write the CSV header
            writer.write("ID, Name, Required Resources, Following Tasks with Requirements, Duration");
            writer.newLine();

            // Write each task line
            for (Task task : tasks) {
                StringBuilder sb = new StringBuilder();
                sb.append(task.getTaskID()).append(",");

                // Write task name or null
                if(!task.getTaskName().equals("")) sb.append(task.getTaskName());
                else sb.append("null");

                // Collect and format required resources
                String res = "";
                for (Resource resource : task.getResources()) {
                    if(!res.equals("")) res += ",";

                    String name = resource.getName();
                    if(name.equals("")) name = "null";
                    String amount = resource.getAmount();
                    if(amount.equals("")) amount = "null";

                    res += "{" + name + "," + amount + "}";
                }

                if(res.equals("")) res="{null,null}";
                sb.append(",{").append(res).append("},");

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

                // Append duration in format [h;m;s]
                sb.append(",{")
                        .append(task.getDuration().get(0))
                        .append(",")
                        .append(task.getDuration().get(1))
                        .append(",")
                        .append(task.getDuration().get(2))
                        .append("}");

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

        // Extract resources, following tasks, and duration arrays
        List<String> resources = new ArrayList<>();
        line = result.get(2).substring(1,result.get(2).length()-1);

        Matcher matcherRes = pattern.matcher(line);
        while (matcherRes.find()) {
            resources.add(matcherRes.group(1).trim());
        }

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

        // Keep taskID unique
        if(taskID<=id) taskID = id+1;

        // Create and add new task UI component
        VBox newTaskGroup = createTaskGroup(colWf, noCommaAndBraces, id, name, resources, followingTasks, duration);
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
        boolean reusable;
        reusable = !content[2].equalsIgnoreCase("false");

        // If the first resource input is still empty, fill it instead of adding a new one
        if(colRes.getChildren().get(2) instanceof HBox
        && ((TextField) ((HBox) colRes.getChildren().get(2)).getChildren().get(0)).getText().equals("")
        && ((TextField) ((HBox) colRes.getChildren().get(2)).getChildren().get(1)).getText().equals("")
        && !((CheckBox) ((HBox) colRes.getChildren().get(2)).getChildren().get(2)).isSelected()) {
            ((TextField) ((HBox) colRes.getChildren().get(2)).getChildren().get(0)).setText(name);
            ((TextField) ((HBox) colRes.getChildren().get(2)).getChildren().get(1)).setText(amount);
            ((CheckBox) ((HBox) colRes.getChildren().get(2)).getChildren().get(2)).setSelected(reusable);
        }
        else {
            // Otherwise, add a new resource input line
            HBox newTextFieldContainer = createResourceField(colRes, name, amount, "","");
            colRes.getChildren().add(colRes.getChildren().size() - 1, newTextFieldContainer);
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
                ((TextField) colSim.getChildren().get(2)).setText(content); // Arrival Rate
                break;
            case 1:
                ((TextField) colSim.getChildren().get(4)).setText(content); // Agents Per Arrival
                break;
            case 2:
                ((TextField) colSim.getChildren().get(6)).setText(content); // Arrival Count
                break;
            case 3:
                // Duration (hours, minutes, seconds)
                String[] con = content.split(",");
                if(!con[0].equals("0"))((TextField)((HBox)((VBox) colSim.getChildren().get(7)).getChildren().get(1)).getChildren().get(0)).setText(con[0]);
                if(!con[1].equals("0"))((TextField)((HBox)((VBox) colSim.getChildren().get(7)).getChildren().get(1)).getChildren().get(1)).setText(con[1]);
                if(!con[2].equals("0"))((TextField)((HBox)((VBox) colSim.getChildren().get(7)).getChildren().get(1)).getChildren().get(2)).setText(con[2]);
                break;
            case 4:
                if(!content.equals("null"))((ComboBox) colSim.getChildren().get(9)).setValue(content); // Mode
                break;
            case 5:
                ((TextField) colSim.getChildren().get(11)).setText(content); // Seed
                break;
            case 6:
                // Resources to measure
                String[] cont = content.split(",");
                int i = 0;
                for(String str : cont) {
                    i++;
                    if(((TextField) ((VBox) colSim.getChildren().get(13)).getChildren().get(1)).getText().equals("") && cont.length>1 && i==1){
                        ((TextField) ((VBox) colSim.getChildren().get(13)).getChildren().get(1)).setText(cont[0]);
                    }
                    else if (!cont[i-1].equals("null")){
                        HBox newTextFieldContainer = createResourceMeasureFieldLine(((VBox) colSim.getChildren().get(13)), cont[i - 1]);
                        ((VBox) colSim.getChildren().get(13)).getChildren().add(newTextFieldContainer); // Add before the save button
                    }
                }
                break;
        }
    }

    private List<String[]> saveTaskToList(List<Task> tasks) {
        // Current form: {ID, Name, first Resource}
        List<String[]> bpmnTaskList = new ArrayList<>();
        for (Task t : tasks){
            String[] x = {"task" + t.getTaskID(), t.getTaskName(), String.valueOf(t.getResources().get(0).getName())};
            bpmnTaskList.add(x);
        }

        return bpmnTaskList;
    }

    private List<String[]> saveConnectionsToList(List<Task> tasks) {
        // Current form: {From, To, Condition}
        List<String[]> bpmnConnectionList = new ArrayList<>();
        int gatewayCnt = 1;
        for (Task t : tasks){
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

    private void createBPMN(List<String[]> taskNames, List<String[]> connections){
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
        for (String[] taskData : taskNames) {
            String id = taskData[0];
            String name = taskData[1];
            String assignee = taskData[2];

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
            addSimulationInfo(file, connections);
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

    private void addSimulationInfo(File bpmnFile, List<String[]> connections) throws Exception {
        // 1) Load BPMN XML file with namespace awareness
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(bpmnFile);

        Element root = doc.getDocumentElement(); // <definitions>

        // 2) Add xmlns:qbp namespace if not present
        String qbpNamespace = "http://www.qbp-simulator.com/Schema201212";



        // 3) Create <qbp:processSimulationInfo> element with attributes
        Element simInfo = doc.createElementNS(qbpNamespace, "qbp:processSimulationInfo");
        simInfo.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:qbp", qbpNamespace);

        simInfo.setAttribute("id", "qbp_e6a1da1e-1b26-b7ae-66cd-ca891d9ea7ef");
        simInfo.setAttribute("processInstances", "4");
        simInfo.setAttribute("startDateTime", "2025-05-22T07:00:00.000Z");
        simInfo.setAttribute("currency", "EUR");

        // Add inner XML (arrivalRateDistribution, timetables, etc.) here as needed
        // For brevity, let's add one example child element:

        Element arrivalRate = doc.createElementNS(qbpNamespace, "qbp:arrivalRateDistribution");
        arrivalRate.setAttribute("type", "FIXED");
        arrivalRate.setAttribute("mean", "5");
        arrivalRate.setAttribute("arg1", "0");
        arrivalRate.setAttribute("arg2", "0");
        Element timeUnit = doc.createElementNS(qbpNamespace, "qbp:timeUnit");
        timeUnit.setTextContent("seconds");
        arrivalRate.appendChild(timeUnit);
        simInfo.appendChild(arrivalRate);

        Element timetables = doc.createElementNS(qbpNamespace, "qbp:timetables");
        Element timetable = doc.createElementNS(qbpNamespace, "qbp:timetable");
        timetable.setAttribute("id", "QBP_DEFAULT_TIMETABLE");
        timetable.setAttribute("default", "true");
        timetable.setAttribute("name", "Default");
        Element rules = doc.createElementNS(qbpNamespace, "qbp:rules");
        Element rule = doc.createElementNS(qbpNamespace, "qbp:rule");
        rule.setAttribute("fromTime","09:00:00.000+00:00");
        rule.setAttribute("toTime","17:00:00.000+00:00");
        rule.setAttribute("fromWeekDay","MONDAY");
        rule.setAttribute("toWeekDay","FRIDAY");
        rules.appendChild(rule);
        timetable.appendChild(rules);
        timetables.appendChild(timetable);
        simInfo.appendChild(timetables);

        List<AvailableResource> resourcesList = collectResourcesData();
        Element resources = doc.createElementNS(qbpNamespace, "qbp:resources");
        for (AvailableResource resourceInstance : resourcesList) {
            Element resource = doc.createElementNS(qbpNamespace, "qbp:resource");
            String id = "Resource" + resourceInstance.getID();
            String name = resourceInstance.getName();
            String totalAmount = resourceInstance.getAmount();
            String costPerHour = "0";
            String timetableId = "QBP_DEFAULT_TIMETABLE";
            resource.setAttribute("id",id);
            resource.setAttribute("name",name);
            resource.setAttribute("totalAmount",totalAmount);
            resource.setAttribute("costPerHour",costPerHour);
            resource.setAttribute("timetableId",timetableId);
            resources.appendChild(resource);
        }
        simInfo.appendChild(resources);

        List<Task> tasksList = collectTaskData();
        Element elements = doc.createElementNS(qbpNamespace, "qbp:elements");
        for (Task taskInstance : tasksList) {
            Element element = doc.createElementNS(qbpNamespace, "qbp:element");
            String id = "elementTask" + taskInstance.getTaskID();
            String elementId = "task" + taskInstance.getTaskID();
            String timeUnitValue = "minutes";
            element.setAttribute("id", id);
            element.setAttribute("elementId", elementId);

            Element durationDistribution = doc.createElementNS(qbpNamespace, "qbp:durationDistribution");
            Integer duration = taskInstance.getDuration().get(1);
            durationDistribution.setAttribute("type", "FIXED");
            if (timeUnitValue.equals("hours")) durationDistribution.setAttribute("mean", String.valueOf(duration*60*60));
            else if (timeUnitValue.equals("minutes")) durationDistribution.setAttribute("mean", String.valueOf(duration*60));
            else  durationDistribution.setAttribute("mean", String.valueOf(duration));
            durationDistribution.setAttribute("arg1", "NaN");
            durationDistribution.setAttribute("arg2", "NaN");
            Element timeUnitTask = doc.createElementNS(qbpNamespace, "qbp:timeUnit");
            timeUnitTask.setTextContent(timeUnitValue);

            Element resourceIds = doc.createElementNS(qbpNamespace, "qbp:resourceIds");
            Element resourceId = doc.createElementNS(qbpNamespace, "qbp:resourceId");
            String resourceID = "";
            for (AvailableResource resourceInstance : resourcesList) {
                if(resourceInstance.getName().equals(taskInstance.getResources().get(0).getName())) resourceID = "Resource" + resourceInstance.getID();
            }
            resourceId.setTextContent(resourceID);

            durationDistribution.appendChild(timeUnitTask);
            element.appendChild(durationDistribution);
            resourceIds.appendChild(resourceId);
            element.appendChild(resourceIds);
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
        simInfo.appendChild(statsOptions);

        // TODO: build the rest of the <qbp:processSimulationInfo> tree as you want

        // 4) Find the <bpmndi:BPMNDiagram> element
        NodeList diagrams = doc.getElementsByTagNameNS("*", "BPMNDiagram");
        if (diagrams.getLength() == 0) {
            throw new RuntimeException("No BPMNDiagram element found in BPMN file!");
        }
        org.w3c.dom.Node diagram = diagrams.item(0);
        org.w3c.dom.Node parent = diagram.getParentNode();

        // 5) Insert simInfo AFTER the <bpmndi:BPMNDiagram> element
        org.w3c.dom.Node next = diagram.getNextSibling();
        if (next != null) {
            parent.insertBefore(simInfo, next);
        } else {
            parent.appendChild(simInfo);
        }

        // 6) Save the updated document back to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(bpmnFile);
        transformer.transform(source, result);

        System.out.println("Simulation info inserted successfully.");
    }
}
/*
// List of task names
List<String[]> taskNames = List.of(
    new String[] {"task1", "TaskName1", "Assignee"},
    new String[] {"task2", "TaskName2"},
    new String[] {"task3", "TaskName3"},
    new String[] {"task4", "TaskName4"},
    new String[] {"task5", "TaskName5"},
    );


// List of connections
List<Object[]> connections = List.of(
    new String[] {"task1", "task2", ""},
    new String[] {"task2", "gateway1", ""},
    new String[] {"gateway1", "task3", "1+1=2"},
    new String[] {"gateway1", "task4", "1+1=3"},
    new String[] {"task3", "task5", ""},
    new String[] {"task4", "task5", ""},
    new String[] {"task5", "end", ""}
);
 */