package com.wynprice.cursemaven


import com.wynprice.cursemaven.repo.CurseMavenRepo
import com.wynprice.cursemaven.repo.CurseRepoDependency
import groovy.transform.TupleConstructor
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
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
 *  @deprecated As of 1.3.0, the resolver is no longer used to manage resources.
 */
@Deprecated
@TupleConstructor class CurseMavenResolver {

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
        println("'curse.resolve($slug, $fileId)' is deprecated. Please use the \"curse.maven:$slug:$fileId\" instead")
        log("Started resolving of slug: $slug, filID: $fileId")
        return resolveUrl("$CurseMavenPlugin.EXTENDED_CURSEFORGE_URL/$slug/files/$fileId")
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
        println("'curse.resolveID($projectID, $fileID)' is deprecated. Please use the \"curse.maven.id:$projectID:$fileID\" instead")
        log("Started resolving of projectID: $projectID, filID: $fileID")
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
        println("'curse.resolveUrl($url)' is deprecated. All calls to this should be changed to \"curse.maven:slug:filID\" instead")

        log("Started resolving of $url")
        //Resolve this base page
        def resolve = resolvePage(url)

        if(!resolve.isValid()) {
            throw new GradleException("Could not resolve dependency for url $url as the format is invalid")
        }

        def fileLocation = resolve.getFolder(CurseMavenRepo.instance.getBaseDir()).getAbsolutePath()
        if(!CurseMavenRepo.instance.isDepCached(resolve)) {
            log("Started Resolving of: $resolve to $fileLocation")
            CurseMavenRepo.instance.putDep(resolve)
        } else {
            log("Dep already cached at $fileLocation, skipping")
        }

        log("Returning dep ${CurseMavenRepo.GROUP_NAME}.${resolve.slug}.${resolve.fileID}")

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
                    def matcher = CurseRepoDependency.URl_PATTERN.matcher"$CurseMavenPlugin.CURSEFORGE_URL${element.attr("href")}"
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
            println "[CurseMavenResolver] $info"
        }
    }

    /**
     * Gets the end result of redirects from the url
     * @param url the starting url
     * @return the end result, after the url is redirected
     */
    private String getRedirect(String url) {
        def page = CurseMavenPlugin.getPage(url)
        def redirect = page.getUrl()
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
        def parse = Jsoup.parse(CurseMavenPlugin.getPage(url).getWebResponse().getContentAsString())
        log "Success downloading $url"
        parse
    }

}
