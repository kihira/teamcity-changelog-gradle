package kihira.changelog;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class ChangelogTask extends DefaultTask {

    private String authHeader;

    @Input String server; //This should include the auth method (ie server/httpAuth or server/guestAuth)
    @Input String projectName;
    @Input boolean showCommit;
    @Input int historyLimit;
    @OutputFile File output;

    @TaskAction
    public void buildChangeLog() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Changelog:").append("\n");

        List<Integer> buildIDs = getBuilds(projectName);
        int build = 0;
        for (int buildID : buildIDs) {
            sb.append(getChanges(buildID));
            build++;
            if (build >= this.historyLimit) break;
        }
        //Add to archives
        Files.write(sb.toString().getBytes(), output);
        getProject().getArtifacts().add("archives", output);
    }

    private List<Integer> getBuilds(String projectLocator) throws IOException {
        String json = read(new URL(this.server + "/app/rest/changes?locator=project:" + projectLocator + ""));
        JsonArray builds = new JsonParser().parse(json).getAsJsonObject().get("build").getAsJsonArray();
        List<Integer> buildIDs = new ArrayList<Integer>();

        for (JsonElement element : builds) {
            //Get global build ID, not project
            buildIDs.add(element.getAsJsonObject().get("id").getAsInt());
        }
        return buildIDs;
    }

    private StringBuilder getChanges(int buildID) throws IOException {
        String json = read(new URL(this.server + "/app/rest/changes?locator=build:(" + buildID + ")"));
        List<Integer> changeIDs = new ArrayList<Integer>();

        //We just want the id's from the change array
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        JsonArray changesArray = jsonObject.get("change").getAsJsonArray();
        for (JsonElement element : changesArray) {
            changeIDs.add(element.getAsJsonObject().get("id").getAsInt());
        }

        //Now we get the actual stuff for changes
        StringBuilder sb = new StringBuilder();
        if (changeIDs.size() > 0) {
            for (int changeID : changeIDs) {
                json = read(new URL(this.server + "/httpAuth/app/rest/changes/id:" + changeID));
                jsonObject = new JsonParser().parse(json).getAsJsonObject();

                //Username
                sb.append("\t").append(jsonObject.get("username").getAsString());
                if (this.showCommit) sb.append(" (").append(jsonObject.get("version").getAsString().substring(0, 7)).append(") ");
                sb.append(": ").append("\n");

                //Changes
                String[] changes = jsonObject.get("comment").getAsJsonArray().getAsString().trim().split("\n");
                for (String line : changes) {
                    sb.append("\t\t").append("* ").append(line).append("\n");
                }
            }
        }
        return sb;
    }

    private String read(URL url) throws IOException {
        URLConnection conn;
        conn = url.openConnection();
        conn.addRequestProperty("Accept", "application/json"); //We want json

        if (!Strings.isNullOrEmpty(this.authHeader)) {
            conn.addRequestProperty("Authorization", this.authHeader);
        }
        return CharStreams.toString(new InputStreamReader(conn.getInputStream()));
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setShowCommit(boolean showCommit) {
        this.showCommit = showCommit;
    }

    public void setHistoryLimit(int historyLimit) {
        this.historyLimit = historyLimit;
    }
}
