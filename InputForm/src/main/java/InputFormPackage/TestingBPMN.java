package InputFormPackage;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.*;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;

import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TestingBPMN {

    public static void main(String[] args) {

        // Task list: id, name, assignee
        List<String[]> taskNames = List.of(
                new String[]{"task1", "TaskName1", "Assignee1"},
                new String[]{"task2", "TaskName2", "Assignee2"},
                new String[]{"task3", "TaskName3", "Assignee3"},
                new String[]{"task4", "TaskName4", "Assignee4"},
                new String[]{"task5", "TaskName5", "Assignee5"}
        );

        // Connections: from, to, condition
        List<String[]> connections = List.of(
                new String[]{"task1", "task2", ""},
                new String[]{"task2", "gateway1", ""},
                new String[]{"gateway1", "task3", "0.5"},
                new String[]{"gateway1", "task4", "0.5"},
                new String[]{"task3", "task5", ""},
                new String[]{"task4", "task5", ""},
                new String[]{"task5", "end", ""}
        );


        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();

        // Create definitions and process
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace("http://example.com/bpmn");


// Set the definitions in the model
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
                } else {
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
        addHorizontalFlowLayout(modelInstance, process, taskNames);

        // Save the BPMN model to file
        File file = new File("generated-BPMN.bpmn");
        Bpmn.writeModelToFile(file, modelInstance);
        System.out.println("BPMN file created: " + file.getAbsolutePath());

        try {
            addSimulationInfo(file, taskNames);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper method to create sequence flows with optional condition
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

    static void addHorizontalFlowLayout(BpmnModelInstance modelInstance, Process process, List<String[]> taskNames) {
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

    static void addSimulationInfo(File bpmnFile, List<String[]> taskNames) throws Exception {
        // 1) Load BPMN XML file with namespace awareness
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(bpmnFile);

        Element root = doc.getDocumentElement(); // <definitions>

        // 2) Add xmlns:qbp namespace if not present
        String qbpNamespace = "http://www.qbp-simulator.com/Schema201212";


        // Use a Set to store unique assignees
        Set<String> uniqueResources = new HashSet<>();

        // Iterate over the task list
        for (String[] task : taskNames) {
            if (task.length >= 3) {
                uniqueResources.add(task[2]);
            }
        }



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
        arrivalRate.setAttribute("mean", "0");
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

        //List<AvailableResource> resourcesList = collectResourcesData();
        //for (AvailableResource resourceInstance : resourcesList) {

            Element resources = doc.createElementNS(qbpNamespace, "qbp:resources");
            Element resource = doc.createElementNS(qbpNamespace, "qbp:resource");
            //String id = "Resource" + resourceInstance.getID();
            //String name = resourceInstance.getName();
            //String totalAmount = resourceInstance.getAmount();
            String costPerHour = "0";
            String timetableId = "QBP_DEFAULT_TIMETABLE";
            //resource.setAttribute("id",id);
            //resource.setAttribute("name",name);
            //resource.setAttribute("totalAmount",totalAmount);
            resource.setAttribute("costPerHour",costPerHour);
            resource.setAttribute("timetableId",timetableId);
            resources.appendChild(resource);
            simInfo.appendChild(resources);
        //}

        Element elements = doc.createElementNS(qbpNamespace, "qbp:elements");
        Element element = doc.createElementNS(qbpNamespace, "qbp:element");
        element.setAttribute("id","qbp_c8ea1917-27b2-d09c-143e-3da52404fcec");
        element.setAttribute("elementId","task1");
        Element durationDistribution = doc.createElementNS(qbpNamespace, "qbp:durationDistribution");
        durationDistribution.setAttribute("type","FIXED");
        durationDistribution.setAttribute("mean","120");
        durationDistribution.setAttribute("arg1","NaN");
        durationDistribution.setAttribute("arg2","NaN");
        Element timeUnit2 = doc.createElementNS(qbpNamespace, "qbp:timeUnit");
        timeUnit2.setTextContent("minutes");
        Element resourceIds = doc.createElementNS(qbpNamespace, "qbp:resourceIds");
        Element resourceId = doc.createElementNS(qbpNamespace, "qbp:resourceId");
        resourceId.setTextContent("ResourceID1");
        durationDistribution.appendChild(timeUnit2);
        element.appendChild(durationDistribution);
        resourceIds.appendChild(resourceId);
        element.appendChild(resourceIds);
        elements.appendChild(element);
        simInfo.appendChild(elements);

        Element sequenceFlows = doc.createElementNS(qbpNamespace, "qbp:sequenceFlows");
        Element sequenceFlow1 = doc.createElementNS(qbpNamespace, "qbp:sequenceFlow");
        sequenceFlow1.setAttribute("elementId","gateway1_to_task3");
        sequenceFlow1.setAttribute("executionProbability","0.3");
        Element sequenceFlow2 = doc.createElementNS(qbpNamespace, "qbp:sequenceFlow");
        sequenceFlow2.setAttribute("elementId","gateway1_to_task4");
        sequenceFlow2.setAttribute("executionProbability","0.7");
        sequenceFlows.appendChild(sequenceFlow1);
        sequenceFlows.appendChild(sequenceFlow2);
        simInfo.appendChild(sequenceFlows);

        Element statsOptions = doc.createElementNS(qbpNamespace, "qbp:statsOptions");
        simInfo.appendChild(statsOptions);

        // TODO: build the rest of the <qbp:processSimulationInfo> tree as you want

        // 4) Find the <bpmndi:BPMNDiagram> element
        NodeList diagrams = doc.getElementsByTagNameNS("*", "BPMNDiagram");
        if (diagrams.getLength() == 0) {
            throw new RuntimeException("No BPMNDiagram element found in BPMN file!");
        }
        Node diagram = diagrams.item(0);
        Node parent = diagram.getParentNode();

        // 5) Insert simInfo AFTER the <bpmndi:BPMNDiagram> element
        Node next = diagram.getNextSibling();
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
