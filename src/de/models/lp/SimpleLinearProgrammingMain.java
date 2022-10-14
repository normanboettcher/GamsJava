package de.models.lp;

import com.gams.api.*;
import de.factories.ModelFactory;

import java.io.File;

/**
 * @author norman
 */
public class SimpleLinearProgrammingMain {
    /**
     * @param args
     */
    public static void main(String[] args) {
        performLPWithoutThreads();
    }

    /**
     *
     */
    private static void performLPWithoutThreads() {
        GAMSWorkspaceInfo gamsWorkspaceInfo = new GAMSWorkspaceInfo();
        gamsWorkspaceInfo.setSystemDirectory("/opt/gams/gams40.3_linux_x64_64_sfx/");
        System.out.println();
        File workingDir = new File(System.getProperty("user.dir"), "LinearProgramming/Example1");
        workingDir.mkdirs();
        gamsWorkspaceInfo.setWorkingDirectory(workingDir.getAbsolutePath());

        GAMSWorkspace gamsWorkspace = new GAMSWorkspace(gamsWorkspaceInfo);
        GAMSJob job = gamsWorkspace.addJobFromFile(ModelFactory.LINEAREPROGRAMMIERUNG_1);
        GAMSDatabase resultDB = gamsWorkspace.addDatabase();

        GAMSOptions opt = gamsWorkspace.addOptions();
        opt.setAllModelTypes("cplex");

        job.run(opt);
        GAMSVariable z = job.OutDB().getVariable("z");
        Double result = Double.valueOf(z.getFirstRecord().getLevel());
        System.out.printf("Result of objective variable z : %s", result);
    }

    /**
     *
     */
    private static void performLPWithThreads() {
        /*
        TBC
         */
    }

}
