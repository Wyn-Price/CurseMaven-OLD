# [CurseMaven](https://login.gradle.org/plugin/com.wynprice.cursemaven) [![Build Status](https://travis-ci.org/Wyn-Price/CurseMaven.svg?branch=master)](https://travis-ci.org/Wyn-Price/CurseMaven)  
Gradle plugin to allow easy access to curseforge files, without using the curseforge maven.
### Curse already has a maven, why does this exist?
I see alot of people saying how curseforge already has a maven and how there's no point in this plugin. While curseforge does indeed have a maven, some of the maven artifacts of the maven (artifact id, version id, classifier ect) all get derived from the jar file name. 

While this is good for if you have a jar properly named (which you should), there can be cases where the jar name is malformed. In these circumstances it can be very difficult to figure out what the maven artifacts should. 
Because each additional file you upload is also added to the curse maven depending on it's file name, you can end up in some situations where if the mod author has uploaded the sources along with their jar, the actual jar and the sources jar end up in different directories, meaning the sources won't be picked up. 

Additionally, if the mod you are trying to use has it's own maven, then you should use that. This is meant as a last resort when the only other option would be using jitpack (which is also an alternative you should consider) or downloading the file and putting it into `/libs/`, which won't work if somebody else clones the repository. 

# Applying the plugin
To see latest version, look [here](https://login.gradle.org/plugin/com.wynprice.cursemaven)
Using the plugins DSL:
```gradle
plugins {
  id "com.wynprice.cursemaven" version "x.x.x"
}
```
Using legacy plugin application:
```gradle
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.wynprice.cursemaven:CurseMaven:x.x.x"
  }
}

apply plugin: "com.wynprice.cursemaven"
```

# Simple usage
```gradle
dependencies {
  compile "curse.maven:jei:2724420" //For versions 2+
  compile curse.resolve("jei", "2724420") //For versions 1.x.x
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/jei/files/2724420), with the scope `compile`

```gradle
dependencies {
  deobfCompile "curse.maven:ctm:2642375" //For versions 2+
  deobfCompile curse.resolve("ctm", "2642375") //For versions 1.x.x
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/ctm/files/2642375), with the scope `deobfCompile`

# Download by ID
`curse.resolveID` allows you to resolve the dependency by the projectID and fileID.
```gradle
dependencies {
  compile "curse.maven.id:238222:2724420" //For versions 2+
  compile curse.resolveID(238222, 2724420) //For versions 1.x.x
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/jei/files/2724420), with the scope `compile`

```gradle
dependencies {
  deobfCompile "curse.maven.id:267602:2642375" //For versions 2+
  deobfCompile curse.resolveID("267602", "2642375") //For versions 1.x.x
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/ctm/files/2642375), with the scope `deobfCompile`

# Download by URL
`curse.resolveUrl` allows you to resolve the dependency by the url (Removed in versions 2+)
```gradle
dependencies {
  compile curse.resolveUrl("https://minecraft.curseforge.com/projects/jei/files/2724420")
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/jei/files/2724420), with the scope `compile`

```gradle
dependencies {
  deobfCompile curse.resolveUrl("https://minecraft.curseforge.com/projects/ctm/files/2642375")
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/ctm/files/2642375), with the scope `deobfCompile`

# Common Problems
### My dependency isn't being resolved with the scope deobfCompile.
(Logs should contain `Could not resolve: deobf.curse_gradle:<slug>:<data>`)
ForgeGradle resolves deobf dependencies when the task `setupDecompWorkspace` is ran. Try re-running `setupDecompWorkspace`, then refreshing the workspace.

# Custom Configuration (Only avliable in versions 1.x.x)
By default, the plugin will download any additional jars with the `source` classifier (`-sources.jar`).    
By default, the plugin will not do any debug logging.    
To change these properties, you need to define your own resolver.
```gradle
import com.wynprice.cursemaven.CurseMavenResolver

def myResolver = new CurseMavenResolver(attachSource: false, debug: true) //Don't attach sources, and allow debug 

dependencies {
  deobfCompile myResolver.resolve("ctm", "2724420")
}
```
The following would download ctm, without looking and downloading (if possible) the sources jar. It also enabled debugging. 

# Special Thanks to 
 - [Tamaized](https://github.com/Tamaized) for working with me to figure out the cloudflare/403 issues.
