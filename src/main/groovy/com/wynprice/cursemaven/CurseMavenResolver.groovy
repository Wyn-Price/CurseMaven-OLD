package com.wynprice.cursemaven

import com.wynprice.cursemaven.repo.CurseMavenRepo
import com.wynprice.cursemaven.repo.CurseRepoDependency
import groovy.transform.TupleConstructor
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import java.util.regex.Pattern

@TupleConstructor class CurseMavenResolver {

    static final String CURSEFORGE_URL = "https://minecraft.curseforge.com"
    static final String SECTION_BREAKER = "----------------------------------------------------------------------------------------------------"
    static final String REQUIRED_DEPENDENCY = "Required Dependency"
    static final Pattern ADDITIONAL_VERSION = Pattern.compile("\\Q<div>\\E(.+?)\\Q</div>\\E")

    static final Logger logger = CurseMavenPlugin.project.logger

    boolean attachSource = true
    boolean debug = false

    Dependency resolve(String slug, String fileVersion, String dependencyScope = null) {
        def url = "$CURSEFORGE_URL/projects/$slug/files/$fileVersion"

        def list = new LinkedList<CurseRepoDependency>()
        def resolve = resolvePage(url, list, dependencyScope != null)

        for (int i = 0; i < list.size(); i++) {
            def dep = list.get(i)

            if(!CurseMavenRepo.instance.isDepCached(dep)) {
                log "Resolved Url: $dep"
                CurseMavenRepo.instance.putDep dep
            }

            if(dependencyScope != null) {
                if(i > 0) {
                    CurseMavenPlugin.project.dependencies.add(dependencyScope, dep.createDependency())
                }
            }

        }
        resolve.createDependency()
    }

    CurseRepoDependency resolvePage(String url, List<CurseRepoDependency> deps, boolean doDeps) {
        def dependency = new CurseRepoDependency(url)

        for (def dep in deps) {
            if(dep.name == dependency.name) {
                return dependency
            }
        }

        def document = getDoc(url)
        if(this.attachSource) {
            def additionalFiles = document.select("a.overflow-tip.twitch-link")
            additionalFiles.forEach { element ->
                if(element.html().endsWith("-sources.jar")) {
                    dependency.sourcesUrl =  "$CURSEFORGE_URL${element.attr("href")}"
                }
            }
        }
        deps.add(dependency)
        if(doDeps) {
            this.downloadDependencies(document, deps)
        }
        dependency
    }

    private void downloadDependencies(Document document, List<CurseRepoDependency> deps) {
        Elements outerElements = document.select("h5:containsOwn($REQUIRED_DEPENDENCY) + ul")
        def mcVersions = new ArrayList<String>()
        document.select("h4:containsOwn(Supported Minecraft) + ul").select("li").forEach {
            mcVersions.add(it.html())
        }
        outerElements.select("a").forEach {
            URLConnection con = new URL("$CURSEFORGE_URL${it.attr("href")}").openConnection() //Used to convert the project id to project slug
            con.setRequestProperty("User-Agent", CurseMavenPlugin.USER_AGENT)
            con.connect()
            InputStream is = con.getInputStream()
            def url = con.getURL().toString()
            is.close()

            def version = getLatestFroMcVersion(url, mcVersions, 1)

            if(version != null) {
                deps.add(new CurseRepoDependency(version))
            }
        }
    }

    private String getLatestFroMcVersion(String subUrl, List<String> mcVersions, int pageID) {
        def url = "$subUrl/files?page=$pageID"
        def doc = getDoc(url)
        def webPageNumber = doc.select("span.s-active")
        def pageNumber = webPageNumber.isEmpty() ? "1" : webPageNumber.get(0).text()
        if(pageNumber != pageID.toString()) {
            println "Unable to find project $subUrl with versions $mcVersions. Checked page $pageID page found $pageNumber"
            return null
        }
        log "$SECTION_BREAKER"
        log "Started analysis of $url with versions $mcVersions"
        for(def element in doc.select("tr.project-file-list-item")) {
            boolean accepted = false
            def verElem = element.select("td.project-file-game-version")
            String mainVer = verElem.select("span.version-label").html()
            def fileLink = element.select("div.project-file-name-container").select("a")
            log "Started to analyse file: ${fileLink.text()}"
            if(mainVer in mcVersions) {
                log "Found main version $mainVer"
                accepted = true
            }
            if(!accepted) {
                log "Started sub-version analysis"
                def elements = verElem.select("span.additional-versions")
                if(!elements.isEmpty()) {
                    def matcher = ADDITIONAL_VERSION.matcher(elements.attr("title"))
                    while (!accepted && matcher.find()) {
                        for (int i = 0; i < matcher.groupCount(); i++) {
                            def versionName = matcher.group(i + 1)
                            log "Testing sub-version $versionName"
                            if(versionName in mcVersions) {
                                log "Found sub-version $versionName"
                                accepted = true
                                break
                            }
                        }
                    }
                }
            }
            if(accepted) {
                def fileExtension = fileLink.attr("href")
                log "Found acceptable url extension: $fileExtension"
                log "$SECTION_BREAKER"
                return "$CURSEFORGE_URL$fileExtension"
            } else {
                log "Could not find version for file ${fileLink.text()}. Skipping"
            }
        }
        log "Could not find acceptable file for url $subUrl with path $pageID"
        log "$SECTION_BREAKER"
        return getLatestFroMcVersion(subUrl, mcVersions, pageID + 1)
    }

    private void log(String info) {
        if(this.debug) {
            println info
        }
    }

    private Document getDoc(String url) {
        log "Downloading file $url"
        logger.info "Downloading: $url"
        Jsoup.connect(url).userAgent(CurseMavenPlugin.USER_AGENT).get()
    }
}
