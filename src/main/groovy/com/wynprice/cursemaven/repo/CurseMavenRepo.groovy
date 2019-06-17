package com.wynprice.cursemaven.repo

import com.wynprice.cursemaven.CurseMavenPlugin
import groovy.transform.TupleConstructor
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@TupleConstructor class CurseMavenRepo  {

    static final String REPO_NAME = "curse_gradle"

    private static CurseMavenRepo INSTANCE

    Path baseDir

    static CurseMavenRepo setInstance(Project project) {
        def path = Paths.get(project.gradle.gradleHomeDir.path, "caches", REPO_NAME)

        project.repositories.maven { MavenArtifactRepository repo ->
            repo.name = REPO_NAME
            repo.url = path.toUri()
        }

        INSTANCE = new CurseMavenRepo(path)
    }

    static CurseMavenRepo getInstance() {
        return INSTANCE
    }

    void putDep(CurseRepoDependency dep) {
        //<group> / <name> / <version> / <name>-<version>.jar
        def folder = this.baseDir.resolve(dep.name).resolve(dep.name).resolve(dep.version).toFile()

        folder.mkdirs()

        println folder

        new File(folder, "${dep.name}-${dep.version}.jar").withOutputStream {
            it.write getBytes("${dep.url}/download")
        }

        if(dep.sourcesUrl != null) {
            new File(folder, "${dep.name}-${dep.version}-sources.jar").withOutputStream {
                it.write getBytes("${dep.sourcesUrl}/download")
            }
        }
    }

    static byte[] getBytes(String url) {
        new URL(url).getBytes([requestProperties: ["User-Agent": CurseMavenPlugin.USER_AGENT]])
    }




}
