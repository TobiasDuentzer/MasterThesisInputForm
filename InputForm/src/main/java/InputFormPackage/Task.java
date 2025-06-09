package InputFormPackage;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private String taskName;
    private Integer taskID;
    private List<Resource> resources;
    private List<FollowingTask> followingTasks;
    private List<Integer> duration;

    public Task(String taskName, Integer taskID, List<Resource> resources, List<FollowingTask> followingTasks, List<Integer> duration) {
        this.taskName = taskName;
        this.taskID = taskID;
        this.resources = resources;
        this.followingTasks = followingTasks;
        this.duration = duration;
    }


    public String getTaskName() {
        return taskName;
    }

    public Integer getTaskID() {
        return taskID;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<FollowingTask> getFollowingTasks() {
        return followingTasks;
    }

    public List<Integer> getDuration() {
        return duration;
    }
}

class FollowingTask {
    private String id;
    private String requirement;

    public FollowingTask(String id, String requirement) {
        this.id = id;
        this.requirement = requirement;
    }

    public String getID() {
        return id;
    }

    public String getRequirement() {
        return requirement;
    }
}

class Resource {
    private String name;
    private String amount;

    public Resource(String name, String amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public String getAmount() {
        return amount;
    }

}



class AvailableResource {
    private String name;
    private String amount;
    private Boolean reusable;
    private String id;

    public AvailableResource(String name, String amount, Boolean reusable, String id) {
        this.name = name;
        this.amount = amount;
        this.reusable = reusable;
        this.id = id;
    }

    public String getName() {
        if(name!="") return name;
        else return "null";
    }

    public String getAmount() {
        if(amount!="") return amount;
        else return "null";
    }

    public Boolean getReusable() {return reusable;}

    public String getID() {
        return id;
    }
}

class Simulation {
    private String arrivalRate;
    private String agentsPerArrival;
    private String arrivalCount;
    private ArrayList<String> duration;
    private String mode;
    private String seed;
    private ArrayList<String> resToMes;

    public Simulation(String arrivalRate, String agentsPerArrival, String arrivalCount, ArrayList<String> duration, String mode, String seed, ArrayList<String> resToMes){
        this.arrivalRate = arrivalRate;
        this.agentsPerArrival = agentsPerArrival;
        this.arrivalCount = arrivalCount;
        this.duration = duration;
        this.mode = mode;
        this.seed = seed;
        this.resToMes = resToMes;
    }

    public String getArrivalRate() {
        return arrivalRate;
    }
    public String getAgentsPerArrival() {
        return agentsPerArrival;
    }
    public String getArrivalCount() {
        return arrivalCount;
    }
    public ArrayList<String> getDuration() {
        return duration;
    }
    public String getMode() {
        return mode;
    }
    public String getSeed() {
        return seed;
    }
    public ArrayList<String> getResToMes() {
        return resToMes;
    }
}
