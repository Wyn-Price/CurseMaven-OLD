package com.wynprice.cursemaven

import com.gargoylesoftware.css.parser.CSSErrorHandler
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
//        client.options.javaScriptEnabled = false
        client.options.SSLClientCipherSuites = ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"]
        client.options.cssEnabled = false
        client.options.throwExceptionOnFailingStatusCode = false
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

        def repos = project.repositories as DefaultRepositoryHandler
        def newMaven = repos.maven { url = "http://www.wynprice.com/dummycursemaven" } as DefaultMavenArtifactRepository
        repos.remove newMaven

        //The proxy is used to allow for the plugin to be used over a variety of gradle versions.
        ArtifactRepository repo = Proxy.newProxyInstance(this.class.classLoader, [ResolutionAwareRepository, ArtifactRepository] as Class[], { proxy, method, args ->

            if(method.name == "createResolver") {
                def resolver = newMaven.createResolver()

                def list = ExternalResourceResolver.class.getDeclaredField("artifactPatterns").identity { setAccessible(true); it }.get(resolver) as List<ResourcePattern>
                list.remove(0)
                list.add(new CurseResourcePattern())

                return resolver
            }
            if(method.name == "setName") {
                //NO-OP, we can't re-set the name
                return Void.class
            }
            return method.invoke(newMaven, args)
        } as InvocationHandler) as ArtifactRepository

        repos.addRepository(repo, "CURSE_DUMMY_REPO")

        CurseMavenPlugin.project = project

        CurseMavenRepo.initialize project
        project.ext.set(VARIABLE_NAME, new CurseMavenResolver())
    }

    static Page getPage(String url) {
        client.getPage url
    }
}
