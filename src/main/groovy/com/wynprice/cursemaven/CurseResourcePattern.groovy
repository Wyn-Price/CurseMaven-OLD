package com.wynprice.cursemaven

import org.gradle.api.GradleException
import org.gradle.api.internal.artifacts.repositories.resolver.M2ResourcePattern
import org.gradle.api.internal.artifacts.repositories.resolver.MavenPattern
import org.jsoup.Jsoup

/**
 * The pattern that's used to take in the slug (or project id) and file id of the curse file,
 * and delegate it to the actual curse maven repository.
 * @author Wyn Price
 */
class CurseResourcePattern extends M2ResourcePattern {

    /**
     * Cache that's used to store the result of {@link #getExtension(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     */
    static final Map<String, String> EXTENSION_CACHE = new HashMap<>()

    static final String DOWNLOAD_URL = "https://edge.forgecdn.net"

    CurseResourcePattern() {
        super(new URI(DOWNLOAD_URL), MavenPattern.M2_PATTERN)
    }

    @Override
    String getPattern() {
        return "Curse Delegate Pattern"
    }

    @Override
    protected String substituteTokens(String pattern, Map<String, String> attributes) {
        //If the organization starts with `curse.`maven, then try and resolve it.
        //Regarding the reversion regex matcher, this can occur when other plugins deobfuscate the dependency and put it in their own repo. IE forge gradle.
        if(attributes.containsKey("organisation") && attributes.get("organisation").startsWith("curse.maven") && attributes.get("revision").matches("^\\d+\$")) {
            return "/files" + getExtension(attributes.get("organisation"), attributes.get("module"), attributes.get("revision"), attributes.get("classifier"))
        }
        return super.substituteTokens(pattern, attributes)
    }

    /**
     * Gets the suffix for {@link #DOWNLOAD_URL}. Used to resolve the URL maven patterns. NOTE: ANY EXCEPTION THROWN BY THIS WILL JUST BE CONSUMED
     * @param group the group of the dependency. Should be either `curse.maven` for normal resolutions or `curse.maven.id` for resolving by id
     * @param artifactID the project slug if {@code group} is `curse.maven`, or the project id if it's `curse.maven.id`
     * @param versionID the file id for the
     * @param classifier the artifact classifier.
     * @return the extension for the given artifacts.
     */
    static String getExtension(String group, String artifactID, String versionID, String classifier) {

        //Gets the cache key for this object. the classifier can be null, hence why Objects.toString is used.
        def cacheKey = "$group:$artifactID:$versionID:${Objects.toString(classifier)}".toString()

        //If the cache exists, return it
        def cache = EXTENSION_CACHE.get(cacheKey)
        if(cache != null) {
            return cache
        }


        //Get the file page url where the file is.
        //For `curse.maven`, it will just be just substituting in the artifactID and versionID
        //For `curse.maven.id`, the project slug will be resolved, then applied in the same was as `curse.maven`
        def url
        switch (group) {
            case "curse.maven":
                url = "$CurseMavenPlugin.EXTENDED_CURSEFORGE_URL/$artifactID/files/$versionID"
                break
            case "curse.maven.id":
                //Get the redirected url given the project id. This allows us to get the project slug
                def redirectUrl = "https://minecraft.curseforge.com/projects/$artifactID"
                def redirect = CurseMavenPlugin.getPage(redirectUrl).getUrl().getPath()
                if(redirectUrl.split("/").last() == redirect.split("/").last()) {
                    def err = "Unknown Project Id $artifactID"
                    println(err)
                    throw new IllegalArgumentException(err)
                }
                url = "$redirect/files/$versionID"
                break
            default:
                //Log the error and throw exception
                def err = "Invalid group: $group. Should be curse.maven or curse.maven.id"
                println err
                throw new IllegalArgumentException(err)
        }

        def fileID = ""
        def fileName = ""


        //jsoup the page so I can parse it
        def doc = Jsoup.parse(CurseMavenPlugin.getPage(url).getWebResponse().getContentAsString())

        //If there isn't a classifier, then just read the file name.
        if(classifier == null) { //No classifier
            def infoBlock = doc.select("span.text-sm.leading-loose + span")
            fileName = infoBlock.get(0).html()
            fileID = versionID
        } else {
            //Get the list of additional files. We can then check which one ends with `-<classifier>.jar` to get the url of the Additional file
            def additionalFilesBlock = doc.select("thead.b-table-header.j-listing-table-header + tbody tr")
            additionalFilesBlock.forEach {
                def element = it.select("td").get(1).select("a")
                if(element.html().endsWith("-${classifier}.jar")) {
                    fileID = element.attr("href").split("/").last()
                    fileName = element.html()
                }
            }
        }

        //If the file id and file name has been found, then get the result from the #DOWNLOAD_URL, and put it in the cache
        if(!fileID.isEmpty() && !fileName.isEmpty()) {
            def result = "/${fileID.substring(0, fileID.length()-3)}/${fileID.substring(fileID.length()-3)}/$fileName"
            EXTENSION_CACHE.put(cacheKey, result)
            return result
        }

        //Log the error and throw exception
        def err = "File ID or File Name could not be found. FileId: '$fileID', FileName: '$fileName', Url '$url";
        println err
        throw new IllegalArgumentException(err)
    }
}

