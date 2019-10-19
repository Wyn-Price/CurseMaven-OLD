package com.wynprice.cursemaven

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver
import org.gradle.api.internal.artifacts.repositories.resolver.ResourcePattern

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * The main plugin class
 * @author Wyn Price
 */
class CurseMavenPlugin implements Plugin<Project> {

    /**
     * The default variable slug that gets added to the project extension
     */
    static final String VARIABLE_NAME = "curse"

    @Override
    void apply(Project project) {

        //Create a new maven from the repository handler, then remove it, so I can delegate to it
        def repos = project.repositories as DefaultRepositoryHandler
        def newMaven = repos.maven { url = "http://www.wynprice.com/dummycursemaven" } as DefaultMavenArtifactRepository
        repos.remove newMaven

        //Create a new repository using the Proxy API. The new repo will implement ResolutionAwareRepository and ArtifactRepository
        //The proxy is used to allow for the plugin to be used over a variety of gradle versions.
        ArtifactRepository repo = Proxy.newProxyInstance(this.class.classLoader, [ResolutionAwareRepository, ArtifactRepository] as Class[], { proxy, method, args ->

            //Delegate the ResolutionAwareRepository#createResolver method.
            if (method.name == "createResolver") {
                //The created resolver to edit
                def resolver = newMaven.createResolver()


                //Get the list of artifact patterns. We then add our own artifact pattern to the list.
                //The new artifact pattern will take in the slug and file id (or project id and file id) and delegate to the actual curse maven repo.
                def list = ExternalResourceResolver.class.getDeclaredField("artifactPatterns").identity {
                    setAccessible(true); it
                }.get(resolver) as List<ResourcePattern>
                list.add(new CurseResourcePattern())

                return resolver
            }

            //Overrides ArtifactRepository#setName
            //I cannot set the name of the repo twice. It is already set from when it was created from the repository handler.
            if (method.name == "setName") {
                //NO-OP, I can't re-set the name
                return Void.class
            }

            //We don't need to override the class. Delegate to the normal method.
            return method.invoke(newMaven, args)
        } as InvocationHandler) as ArtifactRepository

        repos.addRepository(repo, "CURSE_DUMMY_REPO")

        project.ext.set(VARIABLE_NAME, new CurseMavenResolver())
    }
}
