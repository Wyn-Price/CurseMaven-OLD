package com.wynprice.cursemaven

import com.wynprice.cursemaven.repo.CurseMavenRepo
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Pattern

class CurseMavenPlugin implements Plugin<Project> {

    static final String VARIABLE_NAME = "curse"

    static final Pattern URl_PATTERN = Pattern.compile("\\Qhttps://minecraft.curseforge.com/projects/\\E(.+)\\Q/files/\\E(\\d+)")
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1"


    static Project project

    @Override
    void apply(Project project) {
        CurseMavenPlugin.project = project
        CurseMavenRepo.setInstance project
        project.ext.set(VARIABLE_NAME, new CurseMavenResolver())
    }
}
