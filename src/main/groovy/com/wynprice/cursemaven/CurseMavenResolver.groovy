package com.wynprice.cursemaven

import com.wynprice.cursemaven.repo.CurseMavenException
import com.wynprice.cursemaven.repo.CurseMavenRepo
import com.wynprice.cursemaven.repo.CurseRepoDependency
import groovy.transform.TupleConstructor
import org.gradle.api.artifacts.Dependency
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

@TupleConstructor class CurseMavenResolver {

    static final String REQUIRED_LIBRARY = "Required Library"

    boolean attachSource = true
    boolean debug = true

    Dependency resolve(String slug, String fileVersion, boolean sources = true) {
        def url = "https://minecraft.curseforge.com/projects/$slug/files/$fileVersion"

        def resolved = resolvePage url
        log "Resolved Url: $resolved"
        CurseMavenRepo.instance.putDep resolved

        resolved.createDependency()
    }

    CurseRepoDependency resolvePage(String url) {
        def dependency = new CurseRepoDependency(url)
        if(this.attachSource) {
            def document = Jsoup.connect(url).userAgent(CurseMavenPlugin.USER_AGENT).get()
            def additionalFiles = document.select("a.overflow-tip.twitch-link")
            additionalFiles.forEach { element ->
                if(element.html().endsWith("-sources.jar")) {
                    dependency.sourcesUrl =  "https://minecraft.curseforge.com${element.attr("href")}"
                }
            }
        }
        dependency
    }

    private void log(String info) {
        if(this.debug) {
            println info
        }
    }

}
