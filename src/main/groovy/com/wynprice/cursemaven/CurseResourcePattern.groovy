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
        def matcher = attributes.get("revision") =~ /^\d+/
        if(attributes.get("organisation") == "curse.maven" && matcher.find()) {
            try {
                Optional<String> result = getExtension(attributes.get("module"), matcher.group(0), attributes.get("classifier"))
                if(result.isPresent()) {
                    return result.get().substring(DOWNLOAD_URL.length())
                }
            } catch(Exception e) {
                println e.message
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
    static Optional<String> getExtension(String artifactID, String versionID, String classifier) {

        //Gets the cache key for this object. the classifier can be null, hence why Objects.toString is used.
        def cacheKey = "$versionID:${Objects.toString(classifier)}".toString()

        //If the cache exists, return it
        def cache = EXTENSION_CACHE.get(cacheKey)
        if(cache != null) {
            return Optional.of(cache)
        }

        //Get the normal jar result. This should never be empty.
        def result = new URL("https://addons-ecs.forgesvc.net/api/v2/addon/0/file/$versionID/download-url").text
        if(result.isEmpty()) {
            throw new IllegalArgumentException("Version ID is invalid. Artifact: '$artifactID', VersionId: '$versionID'")
        }

        //If we need to search for a classifier, then do so
        if(classifier != null) {
            //Get the normal, no classifier jar name, and the file id for that jar name
            def jarName = result.substring(0, result.length() - ".jar".length())
            def start = Integer.parseInt(versionID)
            boolean found = false

            //Go from the current file version to 20 + the current file version.
            //For each version, get the download url and see if it matches with the found jar name, along with the -classifier prefix.
            //If so then set the result to that and mark the classifier as found
            for(int i = 1; i <= 20; i++) {
                try {
                    def tryResult = new URL("https://addons-ecs.forgesvc.net/api/v2/addon/0/file/${start + i}/download-url").text
                    if(tryResult.endsWith("/$jarName-${classifier}.jar")) {
                        result = tryResult
                        found = true
                        break
                    }
                } catch(FileNotFoundException ignored) {
                    break
                }
            }

            //Classifier could not be found, this is fine.
            if(!found) {
                return Optional.empty()
            }
        }

        EXTENSION_CACHE.put(cacheKey, result)
        Optional.ofNullable(result)
    }
}

