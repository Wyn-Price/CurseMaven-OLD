package com.wynprice.cursemaven

import com.gargoylesoftware.css.parser.CSSErrorHandler
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HTMLParserListener
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener
import com.wynprice.cursemaven.repo.CurseMavenRepo
import org.gradle.api.Plugin
import org.gradle.api.Project

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
     * The user agent used to connect to the curseforge site
     */
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1"

    /**
     * The project instance
     */
    static Project project

    static final WebClient client = new WebClient()

    static {
        client.options.SSLClientCipherSuites = ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"]
        client.options.cssEnabled = false
        client.options.throwExceptionOnFailingStatusCode = false
        client.options.throwExceptionOnScriptError = false

        //TODO: log to a file?
        client.incorrectnessListener = { message, origin -> }
        client.javaScriptErrorListener = ([
                scriptException: { page, exception -> },
                timeoutError: { page, allowedTime, executionTime -> },
                malformedScriptURL: { page, url, malformedURLException -> },
                loadScriptError: { page, scriptUrl, exception -> },
                warn: { page, sourceName, line, lineSource, lineOffset -> },
        ] as JavaScriptErrorListener)
        client.cssErrorHandler = ([
                warning: { exception -> },
                error: { exception -> },
                fatalError: { exception -> }
        ] as CSSErrorHandler)
        client.HTMLParserListener = ([
                error: { message, url, html, line, column, key -> },
                warning: { message, url, html, line, column, key -> }
        ] as HTMLParserListener)

        client.waitForBackgroundJavaScript(30000)
    }

    @Override
    void apply(Project project) {
        CurseMavenPlugin.project = project
        CurseMavenRepo.initialize project
        project.ext.set(VARIABLE_NAME, new CurseMavenResolver())
    }

    static Page getPage(String url) {
        client.getPage url
    }
}
