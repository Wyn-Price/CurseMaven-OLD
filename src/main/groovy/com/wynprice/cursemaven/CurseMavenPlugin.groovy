package com.wynprice.cursemaven

import com.gargoylesoftware.css.parser.CSSErrorHandler
import com.gargoylesoftware.css.parser.CSSException
import com.gargoylesoftware.css.parser.CSSParseException
import com.gargoylesoftware.htmlunit.DefaultCssErrorHandler
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
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
        client.incorrectnessListener = { message, origin -> }
        client.cssErrorHandler = ( [
                warning: { exception -> },
                error: { exception -> },
                fatalError: { exception -> }
        ] as CSSErrorHandler
        )
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
