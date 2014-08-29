package kihira.changelog;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ChangelogPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        final ChangelogTask changelogTask = project.getTasks().create("changelog", ChangelogTask.class);
        changelogTask.setDescription("Creates a changelog for the current build");
    }
}
