package com.wynprice.cursemaven

import com.gargoylesoftware.css.parser.CSSErrorHandler
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HTMLParserListener
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener
import com.wynprice.cursemaven.repo.CurseMavenRepo
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

    /**
     * The base Curseforge URL
     */
    static final String CURSEFORGE_URL = "https://www.curseforge.com"

    /**
     * The base Curseforge URL
     */
    static final String EXTENDED_CURSEFORGE_URL = "$CURSEFORGE_URL/minecraft/mc-mods"


    /**
     * The project instance
     */
    static Project project

    static final WebClient client = new WebClient()

    static {
        //Disable all the unneeded things on the web client.
        client.options.javaScriptEnabled = false
        client.options.SSLClientCipherSuites = ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"]
        client.options.cssEnabled = false
        client.options.throwExceptionOnFailingStatusCode = true
        client.options.throwExceptionOnScriptError = false

        //TODO: log to a file?
        client.incorrectnessListener = { message, origin -> }
        client.javaScriptErrorListener = ([
                scriptException   : { page, exception -> },
                timeoutError      : { page, allowedTime, executionTime -> },
                malformedScriptURL: { page, url, malformedURLException -> },
                loadScriptError   : { page, scriptUrl, exception -> },
                warn              : { page, sourceName, line, lineSource, lineOffset -> },
        ] as JavaScriptErrorListener)
        client.cssErrorHandler = ([
                warning   : { exception -> },
                error     : { exception -> },
                fatalError: { exception -> }
        ] as CSSErrorHandler)
        client.HTMLParserListener = ([
                error  : { message, url, html, line, column, key -> },
                warning: { message, url, html, line, column, key -> }
        ] as HTMLParserListener)

        client.waitForBackgroundJavaScript(30000)
    }

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
            if(method.name == "createResolver") {
                //The created resolver to edit
                def resolver = newMaven.createResolver()


                //Get the list of artifact patterns. We then add our own artifact pattern to the list.
                //The new artifact pattern will take in the slug and file id (or project id and file id) and delegate to the actual curse maven repo.
                def list = ExternalResourceResolver.class.getDeclaredField("artifactPatterns").identity { setAccessible(true); it }.get(resolver) as List<ResourcePattern>
                list.add(new CurseResourcePattern())

                return resolver
            }

            //Overrides ArtifactRepository#setName
            //I cannot set the name of the repo twice. It is already set from when it was created from the repository handler.
            if(method.name == "setName") {
                //NO-OP, I can't re-set the name
                return Void.class
            }

            //We don't need to override the class. Delegate to the normal method.
            return method.invoke(newMaven, args)
        } as InvocationHandler) as ArtifactRepository

        repos.addRepository(repo, "CURSE_DUMMY_REPO")

        CurseMavenPlugin.project = project

        CurseMavenRepo.initialize project
        project.ext.set(VARIABLE_NAME, new CurseMavenResolver())
    }

    /**
     * Gets the curseforge page given the url
     * @param url the url
     * @return the page object for the url
     */
    static Page getPage(String url) {
        //Sometimes, depending on the circumstances, cloudflare will require js to be used.
        //First we should try to get the page without js, as it's faster.
        //If that fails, then try to do it with javascript
        client.options.javaScriptEnabled = false
        try {
            return client.getPage(url)
        } catch(FailingHttpStatusCodeException ignored) {
            client.options.javaScriptEnabled = true
            try {
                return client.getPage(url)
            } catch(Exception e) {
                //When this is called, anything thrown will just be consumed. This is to print out the error to the client.
                e.printStackTrace()
                throw e
            }
        }
    }
}
