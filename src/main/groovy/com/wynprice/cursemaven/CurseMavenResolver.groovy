package com.wynprice.cursemaven

import com.wynprice.cursemaven.repo.CurseMavenRepo
import com.wynprice.cursemaven.repo.CurseRepoDependency
import groovy.transform.TupleConstructor
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import java.util.regex.Pattern

/**
 * Used to resolve curseforge deps from a url. <br>
 * The follwing is a list of options for the configuration:
 * <ul>
 *     <li>
 *         attachSource - if true then the resolver will look for additional artifacts that end with "-source.jar",
 *         and also down load them to the repository, allowing them to be attached as a source. <br> This is by default true
 *     </li>
 *     <li>
 *         debug - if true then the resolver will debug what it is doing as it does it. Useful for when things don't go correctly.
 *    </li>
 * </li>
 *  The following are examples of a configuration:
 *  <pre>
 *      new CurseMavenResolver()                                -> creates a resolver with "attachSource" true and "debug" false
 *      new CurseMavenResolver(attachSource: false)             -> creates a resolver with "attachSource" false and "debug" false
 *      new CurseMavenResolver(attachSource: true, debug:true)  -> creates a resolver with "attachSource" true and "debug" true
 *  </pre>
 *  @author Wyn Price
 */
@TupleConstructor class CurseMavenResolver {

    /**
     * The base Curseforge URL
     */
    static final String CURSEFORGE_URL = "https://minecraft.curseforge.com"

    /**
     * A section breaker. Used for breaking up different recursive calls of code while logging
     */
    static final String SECTION_BREAKER = "----------------------------------------------------------------------------------------------------"

    /**
     * The slug of the required dependency field on the curseforge site
     */
    static final String REQUIRED_DEPENDENCY = "Required Dependency"

    /**
     * The pattern used to divide up <code>&lt;div&gt;$version&lt;/div&gt;</code> html that gets resolved from the curseforge page <br>
     * This is only used for additional versions, not the main versions.
     */
    static final Pattern ADDITIONAL_VERSION = Pattern.compile("\\Q<div>\\E(.+?)\\Q</div>\\E")

    static final Logger logger = CurseMavenPlugin.project.logger

    /**
     * If true, the resolver will look for additional artifacts that end with "-source.jar", and also down
     * load them to the repository, allowing them to be attached as a source. <br> This is by default true
     */
    boolean attachSource = true

    /**
     *  If true then the resolver will debug what it is doing as it does it. Useful for when things don't go correctly.
     */
    boolean debug = false

    /**
     * Resolve a curseforge dependency.
     * @param slug the project's url slug <br>
     *      For example, resolving "https://minecraft.curseforge.com/projects/jei/files/2724420", the project slug would be "jei"
     * @param fileId the projects's file id. <br>
     *     For example, resolving "https://minecraft.curseforge.com/projects/jei/files/2724420", the file id would be "2724420"
     * @param dependencyScope If this is set to anything, then the resolver will try to resolve all dependencies,
     *     including the dependencies dependencies, and those dependencies and so on. The resolver will use this scope as
     *     the scope to use when declaring the dependencies. Dependencies are declared on the curseforge file page, by
     *     adding a "Related Project" with type "Required Dependency". <br> Setting this to null will mean the dependencies don't get resolved
     *     <br>
     *      For example: <pre><code><br>
     *
     *      resolve("jei", "2724420")<br>
     *      would resolve the file at https://minecraft.curseforge.com/projects/jei/files/2724420, <br>
     *      without checking Required Dependencies<br><br>
     *
     *      resolve("jei", "2724420", "deobfCompile")<br>
     *      would resolve the file at https://minecraft.curseforge.com/projects/jei/files/2724420, <br>
     *      and check through the Required Dependencies and resolving them as if they were declared <br>
     *      in the build.gradle dependencies under "deobfCompile" </code></pre> <br><br>
     *
     *      NOTE that the resolved dependencies, as they do not have a version will use the latest version each time. <br>
     *      This is not good practice and can lead to code breaking when it shouldn't<br>
     *      For best practices, you should declare all the dependencies you need, keeping dependencyScope as null.
     *
     *      For this reason, dependencyScope is left @Deprecated
     *
     * @return The base resolved dependency. This is the dependency at the slug and fileID that were provided.
     */
    Dependency resolve(String slug, String fileId, @Deprecated String dependencyScope = null) {
        //Get the formatted url
        def url = "$CURSEFORGE_URL/projects/$slug/files/$fileId"

        def list = new LinkedList<CurseRepoDependency>()
        //Resolve this base page
        def resolve = resolvePage(url, list, dependencyScope != null)

        //Go through all the dependencies and make sure they exist on the dummy maven
        for (int i = 0; i < list.size(); i++) {
            def dep = list.get(i)

            //Make sure the dep is valid before trying to resolve it
            if(!dep.isValid()) {
                continue
            }

            if(!CurseMavenRepo.instance.isDepCached(dep)) {
                log "Resolved Url: $dep"
                CurseMavenRepo.instance.putDep dep
            }

            //If the scope isn't null and it isn't dependency that is resolved normally, then add the dependency to the project
            if (dependencyScope != null && dep != resolve) {
                CurseMavenPlugin.project.dependencies.add(dependencyScope, dep.createDependency())
            }
        }

        //Return the gradle api dependency
        return resolve.createDependency()
    }

    /**
     * Resolve a curseforge page. This passes the url to the CurseRepoDependency.
     * @param url The file page url to be passed in. For example, https://minecraft.curseforge.com/projects/jei/files/2724420
     * @param deps the list of dependencies to add the resolved too
     * @param doDeps Should we calculate/resolve deps.
     * @return the dependency resolved from the <code>url</code>. This is essentially the same as <code>dependencyScope != null</code>
     */
    CurseRepoDependency resolvePage(String url, List<CurseRepoDependency> deps, boolean doDeps) {
        def dependency = new CurseRepoDependency(url)

        //Check to see if this dep has already been resolved.
        //Go through all the other deps and check to see if the project slug is the same
        for (def dep in deps) {
            if(dep.slug == dependency.slug) {
                return dependency
            }
        }

        //Get the document for this page
        def document = getDoc(url)

        //If we attach the sources, then we should look through all the additionalFiles and see which file ends with -sources.jar
        if(this.attachSource) {
            def additionalFiles = document.select("a.overflow-tip.twitch-link") //gets all the additional files
            additionalFiles.forEach { element ->
                //Checks to see if the additional file ends with -sources.jar, and if so set the dependency sourcesUrl to the url of this file
                if(element.html().endsWith("-sources.jar")) {
                    dependency.sourcesUrl =  "$CURSEFORGE_URL${element.attr("href")}"
                }
            }
        }
        //Should we calculate and resolve dependencies
        if(doDeps) {
            this.downloadDependencies(document, deps)
        }
        //Add the dependency to the dependencies list
        deps.add(dependency)
        return dependency
    }

    /**
     * Download all the dependencies from a file page. This looks at the Additional Files section of the file, and goes
     * through all the Required Dependencies, resolving and downloading them.
     * @param document The file page document
     * @param deps the list of dependencies to add too
     */
    private void downloadDependencies(Document document, List<CurseRepoDependency> deps) {
        //Get a list of all minecraft versions. This is taken from the "Supported Minecraft x.y.z Versions" section.
        def mcVersions = new ArrayList<String>()
        document.select("h4:containsOwn(Supported Minecraft) + ul").select("li").forEach {
            mcVersions.add(it.html())
        }


        //Go through all the elements in the Required Dependency section.
        document.select("h5:containsOwn($REQUIRED_DEPENDENCY) + ul").select("a").forEach {

            //Links to all internal mod pages on curseforge are done with project IDs instead of project slugs.
            //Curseforge doesn't allow for access to the "files?page=x" page unless you have the slug as the project slug rather than the project ID.
            //Luckily for us, going to the project ID as the slug simply redirects to the url with the actual slug.
            //Opening a connection and requesting an input stream for that connection is enough to be able to determine
            //where the url redirects to, and we can use that as our new url

            URLConnection con = new URL("$CURSEFORGE_URL${it.attr("href")}").openConnection()
            con.setRequestProperty("User-Agent", CurseMavenPlugin.USER_AGENT)
            con.connect()
            InputStream is = con.getInputStream()
            def url = con.getURL().toString()
            is.close()

            //Get the latest version for this mcVersion
            def version = getLatestFroMcVersion(url, mcVersions, 1)

            if(version != null) {
                deps.add(new CurseRepoDependency(version))
            }
        }
    }

    /**
     * Gets the latest mod for a specifed minecraft version.
     * @param modUrl The base mod page for the dependency that should be resolved.
     * @param mcVersions the accepted minecraft versions.
     * @param page the page that is currently being searched. If no correct version can be found on this page then page will increment until no more pages are left
     * @return
     */
    private String getLatestFroMcVersion(String modUrl, List<String> mcVersions, int page) {
        def url = "$modUrl/files?page=$page"
        def doc = getDoc(url)

        //Gets the active page number on the files page. If there is only one total page then this can be empty
        def webPageNumber = doc.select("span.s-active")

        //Make sure that if it is empty, we set the active page number to one. Otherwise get first element in the list
        //(list contains 2 of the same elements, due to the active page number being at the top and bottom of the screen)
        def pageNumber = webPageNumber.isEmpty() ? "1" : webPageNumber.get(0).text()

        //If the page number being displayed on the web-page doesn't match the page number we requested, then we've gone too far and ran out of pages.
        //This means that the mod doesn't have any acceptable mod versions
        if(pageNumber != page.toString()) {
            println "Unable to find project $modUrl with versions $mcVersions. Checked page $page, page found $pageNumber"
            return null
        }
        log "$SECTION_BREAKER"
        log "Started analysis of $url with versions $mcVersions"

        //Goes through all the files on the current page
        for(def element in doc.select("tr.project-file-list-item")) {
            boolean accepted = false
            def fileLink = element.select("div.project-file-slug-container").select("a") //This is the file name and link that is currently being analysed
            log "Started to analyse file: ${fileLink.text()}"

            //Select the "Game Version" element of the files page
            def versionElement = element.select("td.project-file-game-version")

            //Get the main version from version element on the files page
            String mainVer = versionElement.select("span.version-label").html()

            //If that version is accepted then return it
            if(mainVer in mcVersions) {
                log "Found main version $mainVer"
                accepted = true
            }
            //Otherwise if it isn't accepted then go through and check through all the additional versions
            if(!accepted) {
                log "Started sub-version analysis"
                def elements = versionElement.select("span.additional-versions")
                if(!elements.isEmpty()) {
                    //Additional versions will be in the form `<div>1.13.2</div><div>Java 8</div><div>AnotherVersion</div>`.
                    //Use the regular expression to extract version numbers
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
            //If one of the additional versions is accepted then return it
            if(accepted) {
                def fileExtension = fileLink.attr("href")
                log "Found acceptable url extension: $fileExtension"
                log "$SECTION_BREAKER"
                return "$CURSEFORGE_URL$fileExtension"
            } else {
                log "Could not find version for file ${fileLink.text()}. Skipping"
            }
        }
        log "Could not find acceptable file for url $modUrl with path $page"
        log "$SECTION_BREAKER"

        //There is no acceptable versions on this page. Go to the next page and try again
        return getLatestFroMcVersion(modUrl, mcVersions, page + 1)
    }

    /**
     * Logs a message if <code>debug</code> is enabled
     * @param info
     */
    private void log(String info) {
        if(this.debug) {
            println info
        }
    }

    /**
     * Gets the document for the specified URL
     * @param url the url to get the document from
     * @return the document from that url
     */
    private Document getDoc(String url) {
        log "Downloading file $url"
        logger.info "Downloading: $url"
        Jsoup.connect(url).userAgent(CurseMavenPlugin.USER_AGENT).get()
    }
}
