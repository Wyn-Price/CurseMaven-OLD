package com.wynprice.cursemaven

import com.wynprice.cursemaven.repo.CurseMavenRepo
import com.wynprice.cursemaven.repo.CurseRepoDependency
import groovy.transform.TupleConstructor
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

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
    static final String CURSEFORGE_URL = "https://www.curseforge.com"

    /**
     * The base Curseforge URL
     */
    static final String EXTENDED_CURSEFORGE_URL = "$CURSEFORGE_URL/minecraft/mc-mods"

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
     * Resolve a curseforge dependency from the project slug and file ID.
     * @param slug the project's url slug <br>
     *      For example, resolving "https://minecraft.curseforge.com/projects/jei/files/2724420", the project slug would be "jei"
     * @param fileId the projects's file id. <br>
     *     For example, resolving "https://minecraft.curseforge.com/projects/jei/files/2724420", the file id would be "2724420"
     * @return The resolved dependency. This is the dependency at the slug and fileID that were provided.
     */
    Dependency resolve(def slug, def fileId) {
        return resolveUrl("$EXTENDED_CURSEFORGE_URL/$slug/files/$fileId")
    }

    /**
     * Resolve a curseforge dependency from the project ID and file ID
     * @param projectID the project ID to use. <br>
     *     For example, when resolving a JEI dependency, this would be "238222" (the project ID for JEI)
     * @param fileID the project's file id. <br>
     *     For example, resolving "https://minecraft.curseforge.com/projects/jei/files/2724420", the file id would be "2724420"
     * @return The resolved dependency. This is the dependency at the projectID and fileID that were provided.
     */
    Dependency resolveID(def projectID, def fileID) {
        def url = "https://minecraft.curseforge.com/projects/$projectID"
        def redirect = getRedirect(url)
        if(url == redirect) {
            throw new GradleException("Unknown Project Id $projectID")
        }
        return resolveUrl("$redirect/files/$fileID")
    }

    /**
     * Resolves the curseforge dependency from the url
     * @param url the url to download from. An example would be: "https://minecraft.curseforge.com/projects/jei/files/2724420"
     * @return the resolved dependency at the URL provided
     */
    Dependency resolveUrl(String url) {
        //Resolve this base page
        def resolve = resolvePage(url)


        if(!resolve.isValid()) {
            throw new GradleException("Could not resolve dependency for url $url as the format is invalid")
        }

        if(!CurseMavenRepo.instance.isDepCached(resolve)) {
            log "Resolved Url: $resolve"
            CurseMavenRepo.instance.putDep resolve
        }

        //Return the gradle api dependency
        return resolve.createDependency()
    }

    /**
     * Resolve a curseforge page. This passes the url to the CurseRepoDependency.
     * @param url The file page url to be passed in. For example, https://minecraft.curseforge.com/projects/jei/files/2724420
     * @return the dependency resolved from the <code>url</code>.
     */
    CurseRepoDependency resolvePage(String url) {
        def dependency = new CurseRepoDependency(url)

        //Make sure the dependency isn't already caches
        if(CurseMavenRepo.instance.isDepCached(dependency)) {
            return dependency
        }

        //If we attach the sources, then we should look through all the additionalFiles and see which file ends with -sources.jar
        if(this.attachSource) {
            def additionalFiles = getDoc(url).select("thead.b-table-header.j-listing-table-header + tbody tr") //gets all the additional files
            additionalFiles.forEach { rowElement ->
                def element = rowElement.select("td").get(1).select("a")
                //Checks to see if the additional file ends with -sources.jar, and if so set the dependency sourcesUrl to the url of this file
                if(element.html().endsWith("-sources.jar")) {
                    //Get the file id from the url
                    def matcher = CurseRepoDependency.URl_PATTERN.matcher"$CURSEFORGE_URL${element.attr("href")}"
                    if(matcher.matches()) {
                        dependency.sourcesFileID = matcher.group(2)
                    }
                }
            }
        }

        return dependency
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
     * Gets the end result of redirects from the url
     * @param url the starting url
     * @return the end result, after the url is redirected
     */
    private String getRedirect(String url) {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection()
        con.setRequestProperty("User-Agent", CurseMavenPlugin.USER_AGENT)
        con.setInstanceFollowRedirects(false)
        con.connect()
        def redirect = con.getHeaderField("Location")
        log "Url redirected $url -> $redirect"
        return redirect
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
