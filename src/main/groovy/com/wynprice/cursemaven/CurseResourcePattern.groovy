package com.wynprice.cursemaven

import org.gradle.api.GradleException
import org.gradle.api.internal.artifacts.repositories.resolver.M2ResourcePattern
import org.gradle.api.internal.artifacts.repositories.resolver.MavenPattern
import org.jsoup.Jsoup

class CurseResourcePattern extends M2ResourcePattern {

    static final Map<String, String> EXTENSION_CACHE = new HashMap<>();

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
        if(attributes.containsKey("organisation") && attributes.get("organisation").startsWith("curse.maven")) {
            return "/files" + getExtension(attributes.get("organisation"), attributes.get("module"), attributes.get("revision"), attributes.get("classifier"))
        }
        return super.substituteTokens(pattern, attributes)
    }

    static String getExtension(String group, String artifactID, String versionID, String classifier) {
        def cacheKey = "$group:$artifactID:$versionID:${Objects.toString(classifier)}".toString()
        def cache = EXTENSION_CACHE.get(cacheKey)
        if(cache != null) {
            return cache
        }

        def url
        switch (group) {
            case "curse.maven":
                url = "$CurseMavenPlugin.EXTENDED_CURSEFORGE_URL/$artifactID/files/$versionID"
                break
            case "curse.maven.id":
                def redirectUrl = "https://minecraft.curseforge.com/projects/$artifactID"
                def redirect = CurseMavenPlugin.getPage(redirectUrl).getUrl().getPath()
                if(redirectUrl == redirect) {
                    throw new GradleException("Unknown Project Id $artifactID")
                }
                url = "$redirect/files/$versionID"
                break
            default:
                throw new GradleException("Invalid group: $group. Should be curse.maven or curse.maven.id")
        }

        def fileID = ""
        def fileName = ""

        def doc = Jsoup.parse(CurseMavenPlugin.getPage(url).getWebResponse().getContentAsString())

        if(classifier == null) { //No classifier
            def infoBlock = doc.select("span.text-sm.leading-loose + span")
            fileName = infoBlock.get(0).html()
            fileID = versionID
        } else {
            def additionalFilesBlock = doc.select("thead.b-table-header.j-listing-table-header + tbody tr")
            additionalFilesBlock.forEach {
                def element = it.select("td").get(1).select("a")
                if(element.html().endsWith("-${classifier}.jar")) {
                    fileID = element.attr("href").split("/").last()
                    fileName = element.html()
                }
            }
        }
        if(!fileID.isEmpty() && !fileName.isEmpty()) {
            def result = "/${fileID.substring(0, fileID.length()-3)}/${fileID.substring(fileID.length()-3)}/$fileName"
            EXTENSION_CACHE.put(cacheKey, result)
            return result
        }
        throw new GradleException("File ID or File Name could not be found. FileId: '$fileID', FileName: '$fileName', Url '$url")
    }
}

