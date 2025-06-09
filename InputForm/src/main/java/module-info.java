module com.example.inputform {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires camunda.bpmn.model;
    requires camunda.xml.model;
    requires java.xml;

    //opens com.example.inputform to javafx.fxml;
    opens InputFormPackage to javafx.graphics;
    exports com.example.inputform;
}