package com.wynprice.cursemaven.repo

import com.wynprice.cursemaven.CurseMavenResolver
import groovy.transform.ToString
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

import java.nio.file.Path
import java.util.regex.Pattern

/**
 * A class to store the artifacts for a curse maven dependency
 * @author Wyn Price
 */
@ToString class CurseRepoDependency {

    /**
     * The URL pattern used to extract the project slug and file ID from a url
     */
    static final Pattern URl_PATTERN = Pattern.compile("\\Q$CurseMavenResolver.EXTENDED_CURSEFORGE_URL/\\E(.+)\\Q/files/\\E(\\d+)")

    /**
     * The slug taken from <code>url</code>. Extracted with {@link CurseRepoDependency#URl_PATTERN}
     */
    final String slug

    /**
     * The file id taken from <code>url</code>. Extracted with {@link CurseRepoDependency#URl_PATTERN}
     */
    final String fileID

    /**
     * The file id of the sources that are linked to this dependency
     */
    String sourcesFileID

    /**
     * Create a new dependency from a url. The regular expression {@link CurseRepoDependency#URl_PATTERN} is used to take
     * this url and set <code>slug</code> and <code>fileID</code>
     * @param url The file page url for this dependency
     */
    CurseRepoDependency(String url) {
        def matcher = URl_PATTERN.matcher url
        if(!matcher.matches() || matcher.groupCount() != 2) {
            this.slug = this.fileID = ""
        } else {
            this.slug = matcher.group(1).replaceAll("-", "_")
            this.fileID = matcher.group(2).replaceAll("-", "_")
        }

    }

    /**
     * A dependency is considered valid when the url scheme is in the wrong format,
     * @return whether this dependency is valid
     */
    boolean isValid() {
        !this.slug.isEmpty()
    }

    /**
     * Resolves the folder that the main jar and sources reside in.
     * For example:<code><br>
     * getFolder(Paths.get("/data/myfolder")) -> /data/myfolder/$slug/$fileID/
     * </code>
     * @param baseFolder The base folder to extend off
     * @return The file of where the jars are placed
     */
    File getFolder(Path baseFolder) {
        baseFolder.resolve(this.slug).resolve(this.fileID).toFile()
    }

    /**
     * Resolves the file for a jar, along with a classifier. <br>
     * Assuming that slug = "slug" and fileID = "12345", examples are: <code><br><br>
     * getFile(Paths.get("/myfolder"))                  -> /myfolder/slug/12345/slug-12345.jar<br>
     * getFile(Paths.get("/data/folder"), "sources")    -> /data/folder/slug/12345/slug-12345-sources.jar<br>
     * </code></br><br>
     * @param baseFolder The base folder to extend off
     * @param classifier the classifier for the jar. If ignored then is presumed as no classifier
     * @return the file location of the jar with the specified classifier
     */
    File getFile(Path baseFolder, String classifier = null) {
        new File(getFolder(baseFolder), "${this.slug}-${this.fileID}${classifier!=null?"-$classifier":""}.jar")
    }

    /**
     * Creates the gradle API dependency for this curse dependency.
     * @return The gradle dependency, in the format (group: {@link CurseMavenRepo#GROUP_NAME), name: {@link CurseRepoDependency#slug), version: {@link CurseRepoDependency#fileID)
     */
    Dependency createDependency() {
        new DefaultExternalModuleDependency(CurseMavenRepo.GROUP_NAME, this.slug, this.fileID)
    }

}
