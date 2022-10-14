package de.model.transport;

import com.gams.api.*;
import de.factories.ModelFactory;
import de.factories.SystemDirectoryFactory;

import java.io.File;
import java.util.*;

/**
 * @author norman
 */
public class TransportProblemExample {

    public static void main(String[] args) {
        GAMSWorkspaceInfo workspaceInfo = new GAMSWorkspaceInfo();
        workspaceInfo.setSystemDirectory(SystemDirectoryFactory.SYSTEM_DIRECTORY);
        File workingDir = new File(System.getProperty("user.dir"), "Transport");
        workingDir.mkdir();
        workspaceInfo.setWorkingDirectory(workingDir.getAbsolutePath());

        GAMSWorkspace ws = new GAMSWorkspace(workspaceInfo);

        //prepare input data
        List<String> plants = Arrays.asList("Seattle", "San-Diego");
        List<String> markets = Arrays.asList("New-York", "Chicago", "Topeka");

        Map<String, Double> capacity = new HashMap<String, Double>();
        capacity.put("Seattle", Double.valueOf(325.0));
        capacity.put("San-Diego", Double.valueOf(600.0));

        Map<String, Double> demand = new HashMap<String, Double>();
        demand.put("New-York", Double.valueOf(325.0));
        demand.put("Chicago", Double.valueOf(300.0));
        demand.put("Topeka", Double.valueOf(275.0));

        Map<Vector<String>, Double> distance = new HashMap<Vector<String>, Double>();
        distance.put(new Vector<String>(Arrays.asList(new String[]{"Seattle", "New-York"})), Double.valueOf(2.5));
        distance.put(new Vector<String>(Arrays.asList(new String[]{"Seattle", "Chicago"})), Double.valueOf(1.7));
        distance.put(new Vector<String>(Arrays.asList(new String[]{"Seattle", "Topeka"})), Double.valueOf(1.8));
        distance.put(new Vector<String>(Arrays.asList(new String[]{"San-Diego", "New-York"})), Double.valueOf(2.5));
        distance.put(new Vector<String>(Arrays.asList(new String[]{"San-Diego", "Chicago"})), Double.valueOf(1.8));
        distance.put(new Vector<String>(Arrays.asList(new String[]{"San-Diego", "Topeka"})), Double.valueOf(1.4));

        //add a database and add input data onto the database
        GAMSDatabase database = ws.addDatabase();

        GAMSSet i = database.addSet("i", 1, "canning plants");
        for (String p : plants) {
            i.addRecord(p);
        }
        GAMSSet j = database.addSet("j", 1, "markets");
        for (String m : markets) {
            j.addRecord(m);
        }
        GAMSParameter b = database.addParameter("b", "demand at market j in cases", j);
        for (String m : markets) {
            b.addRecord(m).setValue(demand.get(m));
        }
        GAMSParameter a = database.addParameter("a", "capacity of plant i in cases", i);
        for (String p : plants) {
            a.addRecord(p).setValue(capacity.get(p));
        }


        GAMSParameter d = database.addParameter("d", "distance in thousands of miles", i, j);
        for (Vector<String> vd : distance.keySet()) {
            d.addRecord(vd).setValue(distance.get(vd).doubleValue());
        }
        GAMSParameter f = database.addParameter("f", "freight in dollars per case per thousand miles");
        f.addRecord().setValue(90);

        //create and run a job
        GAMSJob transport = ws.addJobFromFile(ModelFactory.TRANSPORTPROBLEM_BEISPIEL);
        GAMSOptions opt = ws.addOptions();
        opt.defines("gdxincname", database.getName());

        transport.run(opt, database);

        GAMSVariable var = transport.OutDB().getVariable("x");
        for (GAMSVariableRecord rec : var) {
            System.out.println("x(" + rec.getKey(0) + ", " + rec.getKey(1) + "): level="
                    + rec.getLevel() + " marginal=" + rec.getMarginal());
        }
        System.out.println();

        // set option of all model types for xpress and run the job again
        opt.setAllModelTypes("cplex");
        transport.run(opt, database);

        for (GAMSVariableRecord rec : transport.OutDB().getVariable("x")) {
            System.out.println("x(" + rec.getKey(0) + "," + rec.getKey(1) + "): level="
                    + rec.getLevel() + " marginal=" + rec.getMarginal());
        }
    }
}
