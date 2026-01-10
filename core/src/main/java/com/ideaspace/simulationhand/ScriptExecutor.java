package com.ideaspace.simulationhand;

import com.ideaspace.IdeaSpace;

import java.io.File;
import java.io.IOException;

public class ScriptExecutor implements Runnable{

    private IdeaSpace ideaSpace;

    private ProcessBuilder processBuilder;
    private Process process;

    public ScriptExecutor(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public void startPythonScript() {
        processBuilder = new ProcessBuilder("venv/bin/python", "src/modular/simulation_main.py");
        try {
            processBuilder.directory(new File("../python"));
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopPythonScript() {
        if (process != null && process.isAlive()) {
            System.out.println("Stopping Python process...");

            process.destroy();
            try {
                boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

                if (!exited) {
                    System.out.println("Process didn't stop gracefully, forcing termination...");
                    process.destroyForcibly();
                    process.waitFor();
                }

                System.out.println("Python process stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    @Override
    public void run() {
        startPythonScript();
    }

}
