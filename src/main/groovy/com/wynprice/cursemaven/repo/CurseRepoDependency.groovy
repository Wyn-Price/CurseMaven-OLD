package com.wynprice.cursemaven.repo

import com.wynprice.cursemaven.CurseMavenPlugin
import groovy.transform.ToString
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

import java.nio.file.Path

@ToString class CurseRepoDependency {

    final String url
    String sourcesUrl

    final String name
    final String version

    CurseRepoDependency(String url) {
        def matcher = CurseMavenPlugin.URl_PATTERN.matcher url
        if(!matcher.matches() || matcher.groupCount() != 2) {
            throw new CurseMavenException("Illegal URL scheme $url")
        }

        this.url = url
        this.name = matcher.group(1).replaceAll("-", "_")
        this.version = matcher.group(2).replaceAll("-", "_")
    }

    File getFolder(Path baseFolder) {
        baseFolder.resolve(this.name).resolve(this.version).toFile()
    }

    File getFile(Path baseFolder, String classifier = null) {
        new File(getFolder(baseFolder), "${this.name}-${this.version}${classifier!=null?"-$classifier":""}.jar")
    }

    Dependency createDependency() {
        new DefaultExternalModuleDependency(CurseMavenRepo.GROUP_NAME, this.name, this.version)
    }

}
