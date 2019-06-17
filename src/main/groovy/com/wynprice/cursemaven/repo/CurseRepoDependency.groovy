package com.wynprice.cursemaven.repo

import com.wynprice.cursemaven.CurseMavenPlugin
import groovy.transform.ToString
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

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

    Dependency createDependency() {
        new DefaultExternalModuleDependency(this.name, this.name, this.version)
    }

}
