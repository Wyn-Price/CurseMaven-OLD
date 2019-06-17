package com.wynprice.cursemaven.repo

import com.wynprice.cursemaven.CurseMavenPlugin
import groovy.transform.TupleConstructor
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

import java.nio.file.Path
import java.nio.file.Paths

@TupleConstructor class CurseMavenRepo  {

    static final String GROUP_NAME = "curse_gradle"

    private static CurseMavenRepo INSTANCE

    Path baseDir

    static CurseMavenRepo setInstance(Project project) {
        def path = Paths.get(project.gradle.gradleHomeDir.path, "caches")

        project.repositories.maven { MavenArtifactRepository repo ->
            repo.name = GROUP_NAME
            repo.url = path.toUri()
        }

        INSTANCE = new CurseMavenRepo(path.resolve(GROUP_NAME))
    }

    static CurseMavenRepo getInstance() {
        return INSTANCE
    }

    boolean isDepCached(CurseRepoDependency dep) {
        dep.getFile(this.baseDir).exists()
    }

    void putDep(CurseRepoDependency dep) {

        dep.getFolder(this.baseDir).mkdirs()

        dep.getFile(this.baseDir).withOutputStream {
            it.write getBytes("${dep.url}/download")
        }

        if(dep.sourcesUrl != null) {
            dep.getFile(this.baseDir, "sources").withOutputStream {
                it.write getBytes("${dep.sourcesUrl}/download")
            }
        }
    }

    static byte[] getBytes(String url) {
        new URL(url).getBytes([requestProperties: ["User-Agent": CurseMavenPlugin.USER_AGENT]])
    }




}
