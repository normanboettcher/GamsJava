package de.main;

import com.gams.api.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main {

    static int availableThread = Thread.activeCount();
    static int status = 0;

    /**
     * Just an example from official GAMS API Documentation.
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.printf("Threads : %s \n", availableThread);
        GAMSWorkspaceInfo wsInfo = new GAMSWorkspaceInfo();
        wsInfo.setSystemDirectory("/opt/gams/gams40.3_linux_x64_64_sfx/");
        //create a directory
        File workingDir = new File(System.getProperty("user.dir"), "Warehouse");
        workingDir.mkdir();
        wsInfo.setWorkingDirectory(workingDir.getAbsolutePath());

        //create a workspace
        GAMSWorkspace ws = new GAMSWorkspace(wsInfo);

        //create GAMS Database for the results
        GAMSDatabase resultDB = ws.addDatabase();
        resultDB.addParameter("objrep", 1, "objective value");
        resultDB.addSet("supplyMap", 3, "Supply Connection with Level");

        try {
            //run multiple parallel Jobs
            Object dbLock = new Object();
            Map<String, WarehouseThread> warehouseMap = new HashMap<String, WarehouseThread>();

            for (int i = 0; i <= availableThread; i++) {
                WarehouseThread wt = new WarehouseThread(ws, i, resultDB, dbLock);
                warehouseMap.put(Integer.toString(i), wt);
                wt.start();
            }
            // join all threads
            for (WarehouseThread wh : warehouseMap.values()) {
                try {
                    wh.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // export the result database to a GDX file
            resultDB.export(ws.workingDirectory() + GAMSGlobals.FILE_SEPARATOR + "result.gdx");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resultDB.dispose();
        }
    }

    public static void notifyException(GAMSException e) {
        if (e instanceof GAMSExecutionException)
            status = ((GAMSExecutionException) e).getExitCode();
        else
            status = -1;
    }


    static class WarehouseThread extends Thread {
        static String model =
                "$title Warehouse.gms                                                      \n" +
                        "                                                                          \n" +
                        "$eolcom //                                                                \n" +
                        "$SetDDList warehouse store fixed disaggregate // acceptable defines       \n" +
                        "$if not set warehouse    $set warehouse   10                              \n" +
                        "$if not set store        $set store       50                              \n" +
                        "$if not set fixed        $set fixed       20                              \n" +
                        "$if not set disaggregate $set disaggregate 1 // indicator for tighter bigM constraint \n" +
                        "$ife %store%<=%warehouse% $abort Increase number of stores (>%warehouse)  \n" +
                        "                                                                          \n" +
                        "Sets Warehouse  /w1*w%warehouse% /                                        \n" +
                        "     Store      /s1*s%store%     /                                        \n" +
                        "Alias (Warehouse,w), (Store,s);                                           \n" +
                        "Scalar                                                                    \n" +
                        "     fixed        fixed cost for opening a warehouse / %fixed% /          \n" +
                        "Parameter                                                                 \n" +
                        "     capacity(WareHouse)                                                  \n" +
                        "     supplyCost(Store,Warehouse);                                         \n" +
                        "                                                                          \n" +
                        "$eval storeDIVwarehouse trunc(card(store)/card(warehouse))                \n" +
                        "capacity(w)     =   %storeDIVwarehouse% + mod(ord(w),%storeDIVwarehouse%);\n" +
                        "supplyCost(s,w) = 1+mod(ord(s)+10*ord(w), 100);                           \n" +
                        "                                                                          \n" +
                        "Variables                                                                 \n" +
                        "    open(Warehouse)                                                       \n" +
                        "    supply(Store,Warehouse)                                               \n" +
                        "    obj;                                                                  \n" +
                        "Binary variables open, supply;                                            \n" +
                        "                                                                          \n" +
                        "Equations                                                                 \n" +
                        "    defobj                                                                \n" +
                        "    oneWarehouse(s)                                                       \n" +
                        "    defopen(w);                                                           \n" +
                        "                                                                          \n" +
                        "defobj..  obj =e= sum(w, fixed*open(w)) + sum((w,s), supplyCost(s,w)*supply(s,w));  \n" +
                        "                                                                          \n" +
                        "oneWarehouse(s).. sum(w, supply(s,w)) =e= 1;                              \n" +
                        "                                                                          \n" +
                        "defopen(w)..      sum(s, supply(s,w)) =l= open(w)*capacity(w);            \n" +
                        "                                                                          \n" +
                        "$ifthen %disaggregate%==1                                                 \n" +
                        "Equations                                                                 \n" +
                        "     defopen2(s,w);                                                       \n" +
                        "defopen2(s,w).. supply(s,w) =l= open(w);                                  \n" +
                        "$endif                                                                    \n" +
                        "                                                                          \n" +
                        "model distrib /all/;                                                      \n" +
                        "solve distrib min obj using mip;                                          \n" +
                        "abort$(distrib.solvestat<>%SolveStat.NormalCompletion% or                 \n" +
                        "       distrib.modelstat<>%ModelStat.Optimal% and                         \n" +
                        "       distrib.modelstat<>%ModelStat.IntegerSolution%) 'No solution!';    \n" +
                        "                                                                          \n";
        GAMSWorkspace workspace;
        GAMSDatabase result;
        Object lockObject;
        int numberOfWarehouses;

        public WarehouseThread(GAMSWorkspace ws, int number, GAMSDatabase db, Object lockObj) {
            workspace = ws;
            result = db;
            lockObject = lockObj;
            numberOfWarehouses = number;
        }

        public void run() {
            try {
                // instantiate GAMSOptions and define some scalars
                GAMSOptions opt = workspace.addOptions();
                opt.setAllModelTypes("cplex");
                opt.defines("Warehouse", Integer.toString(numberOfWarehouses));
                opt.defines("Store", "65");
                opt.defines("fixed", "22");
                opt.defines("disaggregate", "0");
                opt.setOptCR(0.0); // Solve to optimality

                // create a GAMSJob from string and write results to the result database
                GAMSJob job = workspace.addJobFromString(model);
                job.run(opt, System.out);   // job.run(opt);

                // need to lock database write operations
                synchronized (lockObject) {
                    result.getParameter("objrep").addRecord(Integer.toString(numberOfWarehouses)).setValue(
                            job.OutDB().getVariable("obj").findRecord().getLevel());
                }

                for (GAMSVariableRecord supplyRec : job.OutDB().getVariable("supply")) {
                    if (supplyRec.getLevel() > 0.5)
                        synchronized (lockObject) {
                            String[] keys = new String[]{Integer.toString(numberOfWarehouses), supplyRec.getKey(0), supplyRec.getKey(1)};
                            result.getSet("supplyMap").addRecord(keys);
                        }
                }
            } catch (GAMSException e) {
                e.printStackTrace();
                Main.notifyException(e);
            }
        }
    }
}