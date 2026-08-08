package service;

import java.nio.file.Path;

public final class DesktopShortcutService {
    private DesktopShortcutService() {}

    public static void ensureAsync() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath == null || appPath.isBlank()) return;
        Thread.ofVirtual().start(() -> {
            try {
                Path executable = Path.of(appPath).toAbsolutePath();
                String escaped = executable.toString().replace("'", "''");
                String workingDirectory = executable.getParent().toString().replace("'", "''");
                String command = "$desktop=[Environment]::GetFolderPath('Desktop');" +
                    "$link=Join-Path $desktop 'Asociacion Comunal QA.lnk';" +
                    "$shell=New-Object -ComObject WScript.Shell;" +
                    "$shortcut=$shell.CreateShortcut($link);" +
                    "$shortcut.TargetPath='" + escaped + "';" +
                    "$shortcut.WorkingDirectory='" + workingDirectory + "';" +
                    "$shortcut.IconLocation='" + escaped + ",0';" +
                    "$shortcut.Description='Sistema Asociacion Comunal QA';" +
                    "$shortcut.Save();";
                new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command)
                    .redirectErrorStream(true).start().waitFor();
            } catch (Exception ignored) {
                // Un acceso directo no debe impedir que la aplicacion abra.
            }
        });
    }
}
