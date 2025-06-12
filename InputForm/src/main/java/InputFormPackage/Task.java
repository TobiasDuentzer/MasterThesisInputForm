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

//===========================================BIMP Specific==============================================================

class bimpTask {
    private String taskName;
    private Integer taskID;
    private String resource;
    private List<FollowingTask> followingTasks;
    private bimpDuration duration;
    private bimpCost cost;

    public bimpTask(String taskName, Integer taskID, String resource, List<FollowingTask> followingTasks, bimpDuration duration, bimpCost cost) {
        this.taskName = taskName;
        this.taskID = taskID;
        this.resource = resource;
        this.followingTasks = followingTasks;
        this.duration = duration;
        this.cost = cost;

    }


    public String getTaskName() {
        return taskName;
    }

    public Integer getTaskID() {
        return taskID;
    }

    public String getResource() {
        return resource;
    }

    public List<FollowingTask> getFollowingTasks() {
        return followingTasks;
    }

    public bimpDuration getDuration() {
        return duration;
    }

    public bimpCost getCost() {
        return cost;
    }
}


class bimpDuration {
    private String distribution;
    private String duration;
    private String unit;

    public bimpDuration(String distribution, String duration, String unit) {
        this.distribution = distribution;
        this.duration = duration;
        this.unit = unit;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getDuration() {
        return duration;
    }

    public String getUnit() {
        return unit;
    }

}

class bimpCost {
    private String fixedCost;
    private String costThresh;
    private String durationThresh;
    private String unit;

    public bimpCost(String fixedCost, String costThresh, String durationThresh, String unit) {
        this.fixedCost = fixedCost;
        this.costThresh = costThresh;
        this.durationThresh = durationThresh;
        this.unit = unit;
    }

    public String getFixedCost() {
        return fixedCost;
    }

    public String getCostThresh() {
        return costThresh;
    }

    public String getDurationThresh() {
        return durationThresh;
    }

    public String getUnit() {
        return unit;
    }

}

class bimpAvailableResource {
    private String name;
    private String amount;
    private String cost;
    private String timetable;
    private String ttID;
    private String id;

    public bimpAvailableResource(String name, String amount, String cost, String timetable, String ttID, String id) {
        this.name = name;
        this.amount = amount;
        this.cost = cost;
        this.timetable = timetable;
        this.ttID = ttID;
        this.id = id;
    }

    public String getName() {return name;}

    public String getAmount() {return amount;}

    public String getCost() {return cost;}

    public String getTimetable() {return timetable;}

    public String getTtID() {return ttID;}

    public String getID() {
        return id;
    }
}

class bimpSimulation {
    private bimpArrivalRate arrivalRate;
    private bimpArrivalCount arrivalCount;
    private String startTime;
    private String currency;

    public bimpSimulation(bimpArrivalRate arrivalRate, bimpArrivalCount arrivalCount, String startTime, String currency){
        this.arrivalRate = arrivalRate;
        this.arrivalCount = arrivalCount;
        this.startTime = startTime;
        this.currency = currency;
    }

    public bimpArrivalRate getArrivalRate() {
        return arrivalRate;
    }
    public bimpArrivalCount getArrivalCount() {
        return arrivalCount;
    }
    public String getStartTime() {
        return startTime;
    }
    public String getCurrency() {
        return currency;
    }

}

class bimpArrivalRate {
    private String distribution;
    private String rate;
    private String unit;

    public bimpArrivalRate(String distribution, String rate, String unit) {
        this.distribution = distribution;
        this.rate = rate;
        this.unit = unit;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getRate() {
        return rate;
    }

    public String getUnit() {
        return unit;
    }

}

class bimpArrivalCount {
    private String count;
    private String excludeStart;
    private String excludeEnd;

    public bimpArrivalCount(String count, String excludeStart, String excludeEnd) {
        this.count = count;
        this.excludeStart = excludeStart;
        this.excludeEnd = excludeEnd;
    }

    public String getCount() {
        return count;
    }

    public String getExcludeStart() {
        return excludeStart;
    }

    public String getExcludeEnd() {
        return excludeEnd;
    }

}

class bimpTimetable {
    private String name;
    private String startDay;
    private String endDay;
    private String startTime;
    private String endTime;
    private String id;

    public bimpTimetable(String name, String startDay, String endDay, String startTime, String endTime, String id) {
        this.name = name;
        this.startDay = startDay;
        this.endDay = endDay;
        this.startTime = startTime;
        this.endTime = endTime;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getStartDay() {
        return startDay;
    }

    public String getEndDay() {
        return endDay;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getID() {
        return id;
    }

}