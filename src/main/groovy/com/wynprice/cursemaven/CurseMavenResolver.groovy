package com.wynprice.cursemaven


import groovy.transform.TupleConstructor
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

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
 *  @deprecated As of 2.0.0, the resolver is no longer used to manage resources.
 */
@Deprecated
@TupleConstructor class CurseMavenResolver {

    /**
     * If true, the resolver will look for additional artifacts that end with "-source.jar", and also down
     * load them to the repository, allowing them to be attached as a source. <br> This is by default true
     *
     * This is no longer used
     */
    boolean attachSource = true

    /**
     *  If true then the resolver will debug what it is doing as it does it. Useful for when things don't go correctly.
     *
     *  This is no longer used
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
        new DefaultExternalModuleDependency("curse.maven", String.valueOf(slug), String.valueOf(fileId))
    }


    Dependency resolveID(def projectID, def fileID) {
        println("'curse.resolveID($projectID, $fileID)' is deprecated. Please use the \"curse.maven.id:$projectID:$fileID\" instead")
        new DefaultExternalModuleDependency("curse.maven.id", String.valueOf(projectID), String.valueOf(fileID))
    }

    Dependency resolveUrl(String url) {
        throw new GradleException("resolveURL is no longer supported. Please use curse.maven or curse.maven.id")
    }

}
