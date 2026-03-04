package com.ideaspace.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.ideaspace.IdeaSpace;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;

public class FileLoaderHandler {

    public IdeaSpace ideaSpace;
    public String filePath;
    public FileLoaderHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public String loadCustomModelMac() {
        Thread thread = new Thread(() -> {
            try {
                // Use osascript to show a native macOS file picker
                String[] cmd = {
                    "osascript", "-e",
                    "POSIX path of (choose file of type {\"glb\"} with prompt \"Select a GLB Model\")"
                };

                Process process = Runtime.getRuntime().exec(cmd);
                String filePath = new String(process.getInputStream().readAllBytes()).trim();
                process.waitFor();

                if (!filePath.isEmpty()) {
                    Gdx.app.postRunnable(() -> {
                        this.filePath = filePath;
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();

        return filePath;
    }

    public void loadCustomModelWindows() {
        Thread thread = new Thread(() -> {
            try {
                String[] cmd = {
                    "powershell", "-Command",
                    "Add-Type -AssemblyName System.Windows.Forms;" +
                        "$f = New-Object System.Windows.Forms.OpenFileDialog;" +
                        "$f.Filter = 'GLB Files (*.glb)|*.glb';" +
                        "$f.Title = 'Select a GLB Model';" +
                        "if ($f.ShowDialog() -eq 'OK') { $f.FileName }"
                };

                Process process = Runtime.getRuntime().exec(cmd);
                String filePath = new String(process.getInputStream().readAllBytes()).trim();
                process.waitFor();

                if (!filePath.isEmpty()) {
                    Gdx.app.postRunnable(() -> System.out.println(filePath));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void loadCustomModelLinux() {

        FileChooser fileChooser = new FileChooser(FileChooser.Mode.OPEN);
        fileChooser.setSelectionMode(FileChooser.SelectionMode.FILES);

        ideaSpace.hudPanel.getStage().addActor(fileChooser.fadeIn());

        fileChooser.setListener(new FileChooserAdapter() {
            @Override
            public void selected(Array<FileHandle> files) {
                // Do stuff with the selected file, e.g., load it
                if (!files.isEmpty()) {
                    FileHandle file = files.first();
                    System.out.println("Selected file: " + file.path());
                }
            }
        });


    }

}
