package com.wynprice.cursemaven


import org.gradle.api.internal.artifacts.repositories.resolver.M2ResourcePattern
import org.gradle.api.internal.artifacts.repositories.resolver.MavenPattern

/**
 * The pattern that's used to take in the slug (or project id) and file id of the curse file,
 * and delegate it to the actual curse maven repository.
 * @author Wyn Price
 */
class CurseResourcePattern extends M2ResourcePattern {

    /**
     * Cache that's used to store the result of {@link #getExtension(java.lang.String, java.lang.String, java.lang.String)}
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
        //If the organization is equal to `curse.`maven, then try and resolve it.
        //Regarding the reversion regex matcher, this can occur when other plugins deobfuscate the dependency and put it in their own repo. IE forge gradle.
        if(attributes.get("organisation") == "curse.maven" && attributes.get("revision").matches("^\\d+\$")) {
            try {
                return getExtension(attributes.get("module"), attributes.get("revision"), attributes.get("classifier")).substring(DOWNLOAD_URL.length())
            } catch(Exception e) {
                e.printStackTrace()
            }
        }
        return super.substituteTokens(pattern, attributes)
    }

    /**
     * Gets the suffix for {@link #DOWNLOAD_URL}. Used to resolve the URL maven patterns. NOTE: ANY EXCEPTION THROWN BY THIS WILL JUST BE CONSUMED
     * @param artifactID the project slug if {@code group} is `curse.maven`, or the project id if it's `curse.maven.id`
     * @param versionID the file id for the
     * @param classifier the artifact classifier.
     * @return the extension for the given artifacts.
     */
    static String getExtension(String artifactID, String versionID, String classifier) {

        //Gets the cache key for this object. the classifier can be null, hence why Objects.toString is used.
        def cacheKey = "$versionID:${Objects.toString(classifier)}".toString()

        //If the cache exists, return it
        def cache = EXTENSION_CACHE.get(cacheKey)
        if(cache != null) {
            return cache
        }

        def result = ""
        if(classifier == null) {
            result = new URL("https://addons-ecs.forgesvc.net/api/v2/addon/0/file/$versionID/download-url").text
        } else {
            def start = Integer.parseInt(versionID)
            for(int i = 1; i <= 20; i++) {
                def tryResult = new URL("https://addons-ecs.forgesvc.net/api/v2/addon/0/file/${start + i}/download-url").text
                if(tryResult.endsWith("-${classifier}.jar")) {
                    result = tryResult
                    break
                }
            }
        }


        if(result.isEmpty()) {
            def err = "Version ID is invalid. Artifact: '$artifactID', VersionId: '$versionID'"
            println err
            throw new IllegalArgumentException(err)
        }

        EXTENSION_CACHE.put(cacheKey, result)
        return result
    }
}

