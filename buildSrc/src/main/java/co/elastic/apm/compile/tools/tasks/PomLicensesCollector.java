package co.elastic.apm.compile.tools.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import co.elastic.apm.compile.tools.data.ArtifactLicense;
import co.elastic.apm.compile.tools.utils.PomReader;

public abstract class PomLicensesCollector extends DefaultTask {

    @InputFiles
    public abstract Property<Configuration> getRuntimeDependencies();

    @Optional
    @InputFile
    public abstract Property<File> getManualLicenseMapping();

    @OutputFile
    public abstract RegularFileProperty getLicensesFound();

    @SuppressWarnings("unchecked")
    @TaskAction
    public void action() {
        ArtifactResolutionResult result = getProject().getDependencies().createArtifactResolutionQuery()
                .forComponents(getComponentIdentifiers())
                .withArtifacts(MavenModule.class, MavenPomArtifact.class)
                .execute();

        try {
            List<ArtifactLicense> artifactLicenses = extractLicenses(getPomArtifacts(result));
            File licensesFoundFile = getLicensesFound().get().getAsFile();
            writeToFile(licensesFoundFile, artifactLicenses);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getManualMappedLicenses() {
        Map<String, String> mappedLicenses = new HashMap<>();
        File manualMappingFile = getManualLicenseMapping().getOrNull();

        if (manualMappingFile == null) {
            return mappedLicenses;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(manualMappingFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                String dependencyUri = parts[0];
                String licenseId = parts[1];
                if (mappedLicenses.containsKey(dependencyUri)) {
                    throw new RuntimeException("Duplicated dependency license mapping for: " + licenseId);
                }
                mappedLicenses.put(dependencyUri, licenseId);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mappedLicenses;
    }

    private void writeToFile(File licensesFoundFile, List<ArtifactLicense> artifactLicenses) throws IOException {
        FileWriter fileWriter = new FileWriter(licensesFoundFile);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        boolean firstIteration = true;

        for (ArtifactLicense artifactLicense : artifactLicenses) {
            if (!firstIteration) {
                printWriter.println();
            } else {
                firstIteration = false;
            }
            printWriter.print(artifactLicense.serialize());
        }

        printWriter.close();
    }

    private List<ArtifactLicense> extractLicenses(Map<ComponentIdentifier, ResolvedArtifactResult> pomArtifacts) throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> manualMappedLicenses = getManualMappedLicenses();
        List<ArtifactLicense> artifactLicenses = new ArrayList<>();
        List<String> notFoundLicenses = new ArrayList<>();

        for (ComponentIdentifier pomArtifactKey : pomArtifacts.keySet()) {
            String displayName = pomArtifactKey.getDisplayName();
            String licenseName;
            if (manualMappedLicenses.containsKey(displayName)) {
                licenseName = manualMappedLicenses.get(displayName);
            } else {
                ResolvedArtifactResult pomArtifact = pomArtifacts.get(pomArtifactKey);
                File pomFile = pomArtifact.getFile();
                PomReader reader = new PomReader(pomFile);
                licenseName = reader.getLicenseName();
            }
            if (licenseName != null) {
                artifactLicenses.add(new ArtifactLicense(displayName, licenseName));
            } else {
                notFoundLicenses.add(displayName);
            }
        }

        if (!notFoundLicenses.isEmpty()) {
            throw new RuntimeException("Could not find a license in the POM file for: " + notFoundLicenses);
        }

        return artifactLicenses;
    }

    private Map<ComponentIdentifier, ResolvedArtifactResult> getPomArtifacts(ArtifactResolutionResult result) {
        Map<ComponentIdentifier, ResolvedArtifactResult> results = new HashMap<>();

        for (ComponentArtifactsResult component : result.getResolvedComponents()) {
            Set<ArtifactResult> artifacts = component.getArtifacts(MavenPomArtifact.class);
            ComponentIdentifier id = component.getId();
            String displayName = id.getDisplayName();
            if (!artifacts.iterator().hasNext()) {
                throw new RuntimeException("No POM file found for: " + displayName);
            }
            ArtifactResult artifact = artifacts.iterator().next();
            results.put(id, (ResolvedArtifactResult) artifact);
        }

        return results;
    }

    private List<ComponentIdentifier> getComponentIdentifiers() {
        List<String> externalDependenciesIds = new ArrayList<>();

        for (Dependency dependency : getRuntimeDependencies().get().getAllDependencies()) {
            if (dependency instanceof ExternalModuleDependency) {
                ExternalModuleDependency moduleDependency = (ExternalModuleDependency) dependency;
                externalDependenciesIds.add(moduleDependency.getGroup() + ":" + moduleDependency.getName());
            }
        }

        Set<ResolvedArtifact> resolvedArtifacts = getRuntimeDependencies().get().getResolvedConfiguration().getResolvedArtifacts();
        List<ComponentIdentifier> identifiers = new ArrayList<>();

        for (ResolvedArtifact resolvedArtifact : resolvedArtifacts) {
            ModuleVersionIdentifier moduleId = resolvedArtifact.getModuleVersion().getId();
            String moduleIdName = moduleId.getGroup() + ":" + moduleId.getName();
            if (externalDependenciesIds.contains(moduleIdName)) {
                externalDependenciesIds.remove(moduleIdName);
                identifiers.add(resolvedArtifact.getId().getComponentIdentifier());
            }
        }

        if (!externalDependenciesIds.isEmpty()) {
            throw new RuntimeException("POM files not found for the following dependencies: " + externalDependenciesIds);
        }

        return identifiers;
    }
}